/*******************************************************************************
 * Copyright (c) 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.index;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.PendingDeletion;
import net.sourceforge.docfetcher.model.TreeIndex.IndexingResult;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.index.Task.TaskState;
import net.sourceforge.docfetcher.model.index.file.FileIndex;
import net.sourceforge.docfetcher.model.index.outlook.OutlookIndex;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.NotThreadSafe;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.collect.LazyList;

import com.google.common.collect.ImmutableList;

/**
 * @author Tran Nam Quang
 */
public final class IndexingQueue {

	public interface ExistingTasksHandler {
		public void handleExistingTasks(@NotNull List<Task> tasks);
	}

	public enum Rejection {
		INVALID_UPDATE,
		OVERLAP_WITH_REGISTRY,
		OVERLAP_WITH_QUEUE,
		SAME_IN_REGISTRY,
		SAME_IN_QUEUE,
		REDUNDANT_UPDATE,
		SHUTDOWN,
	}
	
	// may be called from a different thread
	public final Event<Void> evtQueueEmpty = new Event<Void>();
	public final Event<Void> evtWorkerThreadTerminated = new Event<Void>();

	private final Event<Task> evtAdded = new Event<Task>();
	private final Event<Task> evtRemoved = new Event<Task>();

	private final Thread thread;
	private final IndexRegistry indexRegistry;
	private final LinkedList<Task> tasks = new LinkedList<Task>(); // guarded by lock

	private volatile boolean shutdown = false; // guarded by lock
	final Lock readLock;
	final Lock writeLock;
	private final Condition readyTaskAvailable;
	final int reporterCapacity;

	public IndexingQueue(	@NotNull final IndexRegistry indexRegistry,
							int reporterCapacity) {
		this.indexRegistry = indexRegistry;
		this.reporterCapacity = reporterCapacity;
		
		readLock = indexRegistry.getReadLock();
		writeLock = indexRegistry.getWriteLock();
		readyTaskAvailable = writeLock.newCondition();
		
		/*
		 * In case of rebuild tasks, if a task is removed before it has entered
		 * the indexing state, put the associated index back into the registry.
		 * If the task has already entered the indexing state, putting the index
		 * back into the registry is the responsibility of the worker thread.
		 */
		evtRemoved.add(new Event.Listener<Task>() {
			public void update(Task task) {
				boolean isQueueEmpty;
				LuceneIndex luceneIndex = task.getLuceneIndex();
				writeLock.lock();
				try {
					boolean isRebuild = task.is(IndexAction.REBUILD);
					boolean notStartedYet = task.is(TaskState.NOT_READY) || task.is(TaskState.READY);
					if (isRebuild && notStartedYet) {
						assert !indexRegistry.getIndexes().contains(luceneIndex);
						indexRegistry.addIndex(luceneIndex);
					}
					isQueueEmpty = tasks.isEmpty();
				}
				finally {
					writeLock.unlock();
				}
				if (isQueueEmpty)
					evtQueueEmpty.fire(null);
			}
		});
		
		thread = new Thread(IndexingQueue.class.getName()) {
			public void run() {
				while (threadLoop());
				evtWorkerThreadTerminated.fire(null);
			}
		};
		thread.start();
	}
	
	// returns whether the loop should continue
	private boolean threadLoop() {
		// Wait for next task
		Task task;
		writeLock.lock();
		try {
			task = getReadyTask();
			while (task == null && !shutdown) {
				readyTaskAvailable.await();
				task = getReadyTask();
			}
			if (shutdown)
				return false;
		}
		catch (InterruptedException e) {
			// Do not interrupt this thread, call Condition.signal*() instead.
			throw new IllegalStateException();
		}
		finally {
			writeLock.unlock();
		}

		assert isValidRegistryState(indexRegistry, task);

		// Indexing
		task.set(TaskState.INDEXING);
		LuceneIndex luceneIndex = task.getLuceneIndex();
		if (task.is(IndexAction.REBUILD)) {
			/*
			 * If the task is a rebuild, the searcher will be holding on to the
			 * underlying index at this point, since it doesn't care whether the
			 * index was removed from the registry or not. Therefore, before
			 * clearing the index, we must signal the searcher to let go of it
			 * by refreshing the searcher's internal Lucene searcher.
			 */
			indexRegistry.getSearcher().replaceLuceneSearcher();
			luceneIndex.clear();
		}
		IndexingResult result = task.update(); // Long-running process
		boolean hasErrors = luceneIndex.hasErrorsDeep();

		boolean doDelete = false;
		boolean fireRemoved = false;
		
		// Post-processing
		writeLock.lock();
		try {
			if (task.is(IndexAction.UPDATE)) {
				/*
				 * In case of index updates, save to disk even if the indexing
				 * fails. An alternative is to automatically discard the index,
				 * which would probably confuse and/or annoy the user, because
				 * then indexes could magically disappear at any time.
				 */
				assert !task.is(CancelAction.DISCARD);
				if (task.getDeletion() == null
						|| result != IndexingResult.SUCCESS_UNCHANGED) {
					indexRegistry.save(luceneIndex);
					indexRegistry.getSearcher().replaceLuceneSearcher();
				}
				else {
					doDelete = true;
				}
				fireRemoved = tasks.remove(task);
			}
			else if (result == IndexingResult.FAILURE) {
				doDelete = true;
			}
			else if (task.is(CancelAction.DISCARD)) {
				doDelete = true;
				fireRemoved = tasks.remove(task);
			}
			else {
				indexRegistry.addIndex(luceneIndex);
				if (result == IndexingResult.SUCCESS_CHANGED)
					indexRegistry.save(luceneIndex);
				boolean keep = task.is(CancelAction.KEEP);
				if (keep || shutdown || !hasErrors)
					fireRemoved = tasks.remove(task);
			}
			task.set(TaskState.FINISHED);
		}
		finally {
			writeLock.unlock();
		}
		
		if (fireRemoved)
			evtRemoved.fire(task);
		
		task.evtFinished.fire(hasErrors);
		
		// Delete index; this can be done without holding the lock
		if (doDelete) {
			/*
			 * Note: This is a typical check-then-act situation that would
			 * normally require holding the lock. However, in this case the lock
			 * is not needed: The task must either have been removed from the
			 * task queue when the lock was held, or it was not an update task.
			 * In both cases, changing the deletion field at this point should
			 * not be not possible.
			 */
			PendingDeletion deletion = task.getDeletion();
			if (deletion == null) {
				luceneIndex.delete();
			}
			else {
				assert task.is(IndexAction.UPDATE);
				deletion.setApprovedByQueue();
			}
		}
		
		return true;
	}

	@NotThreadSafe
	@Nullable
	private Task getReadyTask() {
		for (Task task : tasks)
			if (task.is(TaskState.READY) && task.cancelAction == null)
				return task;
		return null;
	}

	@ThreadSafe
	private static boolean isValidRegistryState(@NotNull IndexRegistry indexRegistry,
												@NotNull Task task) {
		LuceneIndex luceneIndex = task.getLuceneIndex();
		boolean registered = indexRegistry.getIndexes().contains(luceneIndex);
		return registered == task.is(IndexAction.UPDATE);
	}

	// Returns whether the task was added
	@Nullable
	@ThreadSafe
	public Rejection addTask(	@NotNull LuceneIndex index,
								@NotNull IndexAction action) {
		Util.checkNotNull(index, action);
		Util.checkThat(index instanceof FileIndex
				|| index instanceof OutlookIndex);
		
		Task task = new Task(this, index, action);

		// Check that the given index has the right index directory
		File taskIndexDir = task.getLuceneIndex().getIndexDirPath().getCanonicalFile();
		File taskParentIndexDir = Util.getParentFile(taskIndexDir);
		File indexParentDir = indexRegistry.getIndexParentDir();
		String absPath1 = Util.getAbsPath(taskParentIndexDir);
		String absPath2 = Util.getAbsPath(indexParentDir);
        if (Util.IS_WINDOWS) {
        	// Bug #3536137: On Windows, filenames are case-insensitive
            absPath1 = absPath1.toUpperCase();
            absPath2 = absPath2.toUpperCase();
        }
		Util.checkThat(absPath1.equals(absPath2), absPath1 + " != " + absPath2);
		
		LazyList<Task> removedTasks = new LazyList<Task>();

		writeLock.lock();
		try {
			assert task.cancelAction == null;
			assert task.is(TaskState.NOT_READY) || task.is(TaskState.READY);
			if (shutdown)
				return Rejection.SHUTDOWN;

			List<LuceneIndex> indexesInRegistry = indexRegistry.getIndexes();

			if (task.is(IndexAction.UPDATE)) {
				/*
				 * Reject update requests for indexes that are not (or no
				 * longer) in the registry. Such requests may be caused by
				 * obsolete folder watching events.
				 */
				if (!indexesInRegistry.contains(index))
					return Rejection.INVALID_UPDATE;

				/*
				 * Here, we reject a request to enqueue an update task if there
				 * is already another task in the queue that has the same target
				 * file or directory and is in ready state.
				 * 
				 * This is not a bullet-proof way to avoid unnecessary updates
				 * while ensuring that any necessary updates are run: Going
				 * through the queue, we could find a ready task with a matching
				 * target, and reject the enqueue request based on that, but the
				 * user could later cancel the ready task, thus skipping an
				 * update that should have been run. However, the approach here
				 * should work well enough, assuming that it is very unlikely
				 * that the user will cancel ready tasks.
				 */
				for (Task queueTask : tasks)
					if (queueTask.is(TaskState.READY)
							&& sameTarget(queueTask, task))
						return Rejection.REDUNDANT_UPDATE;
			}
			else if (index instanceof OutlookIndex) {
				/*
				 * Reject a request to create or rebuild an Outlook index if
				 * it has the same PST file as another Outlook index in the
				 * registry.
				 */
				for (LuceneIndex index0 : indexesInRegistry)
					if (index0 instanceof OutlookIndex
							&& sameTarget(index0, task))
						return Rejection.SAME_IN_REGISTRY;

				/*
				 * Reject a request to create or rebuild an Outlook index if
				 * it has the same PST file as an Outlook index in the
				 * queue. Exception: Rebuild tasks may replace existing
				 * update tasks on the same PST file, causing the update
				 * tasks to be cancelled.
				 */
				Iterator<Task> it = tasks.iterator();
				while (it.hasNext()) {
					Task queueTask = it.next();
					assert queueTask != task;
					if (!(queueTask.getLuceneIndex() instanceof OutlookIndex))
						continue;

					if (task.is(IndexAction.REBUILD)
							&& queueTask.is(IndexAction.UPDATE)) {
						if (sameTarget(queueTask, task)) {
							assert index == queueTask.getLuceneIndex();
							if (queueTask.is(TaskState.INDEXING))
								queueTask.cancelAction = CancelAction.KEEP;
							it.remove();
							removedTasks.add(queueTask);
						}
					}
					else if (sameTarget(queueTask, task)) {
						return Rejection.SAME_IN_QUEUE;
					}
				}
			}
			else {
				/*
				 * Reject a request to create or rebuild a file index if it
				 * overlaps with a file index in the registry.
				 */
				assert index instanceof FileIndex;
				for (LuceneIndex index0 : indexesInRegistry) {
					if (index0 instanceof OutlookIndex)
						continue;
					File f1 = index0.getCanonicalRootFile();
					File f2 = task.getLuceneIndex().getCanonicalRootFile();
					if (f1.equals(f2))
						return Rejection.SAME_IN_REGISTRY;
					if (isOverlapping(f1, f2))
						return Rejection.OVERLAP_WITH_REGISTRY;
				}

				/*
				 * Reject a request to create or rebuild a file index if it
				 * overlaps with a file index in the queue. Exception:
				 * Rebuild tasks may replace existing update tasks on the
				 * same index, causing the update tasks to be cancelled.
				 */
				Iterator<Task> it = tasks.iterator();
				while (it.hasNext()) {
					Task queueTask = it.next();
					assert queueTask != task;
					if (!(queueTask.getLuceneIndex() instanceof FileIndex))
						continue;

					File f1 = queueTask.getLuceneIndex().getCanonicalRootFile();
					File f2 = index.getCanonicalRootFile();

					if (isOverlapping(f1, f2))
						return Rejection.OVERLAP_WITH_QUEUE;

					if (task.is(IndexAction.REBUILD)
							&& queueTask.is(IndexAction.UPDATE)) {
						if (f1.equals(f2)) {
							if (queueTask.is(TaskState.INDEXING))
								queueTask.cancelAction = CancelAction.KEEP;
							it.remove();
							removedTasks.add(queueTask);
						}
					}
					else if (f1.equals(f2)) {
						return Rejection.SAME_IN_QUEUE;
					}
				}
			}

			tasks.add(task);
			if (task.is(TaskState.READY))
				readyTaskAvailable.signal();
		}
		finally {
			writeLock.unlock();
		}
		
		evtAdded.fire(task);
		for (Task removedTask : removedTasks)
			evtRemoved.fire(removedTask);
		
		return null;
	}
	
	@NotThreadSafe
	static boolean sameTarget(@NotNull Task task1, @NotNull Task task2) {
		File target1 = task1.getLuceneIndex().getCanonicalRootFile();
		File target2 = task2.getLuceneIndex().getCanonicalRootFile();
		return target1.equals(target2);
	}

	@NotThreadSafe
	private static boolean sameTarget(	@NotNull LuceneIndex index,
										@NotNull Task task) {
		File target1 = index.getCanonicalRootFile();
		File target2 = task.getLuceneIndex().getCanonicalRootFile();
		return target1.equals(target2);
	}

	@NotThreadSafe
	private static boolean isOverlapping(@NotNull File f1, @NotNull File f2) {
		return Util.contains(f1, f2) || Util.contains(f2, f1);
	}

	// see Task.remove(CancelHandler)
	// The listeners are not detached if the cancel handler returns null on the active task
	// Warning: Cancel handler is called under lock, so caller must take possible
	// lock-ordering deadlocks into account.
	// The given removed listener is *not* notified of any removed tasks.
	@ThreadSafe
	public void removeAll(	@NotNull CancelHandler handler,
							@NotNull Event.Listener<Task> addedListener,
							@NotNull Event.Listener<Task> removedListener) {
		Util.checkNotNull(handler, addedListener, removedListener);
		LazyList<Task> removedTasks = new LazyList<Task>();
		
		writeLock.lock();
		try {
			if (!removeAll(handler, removedTasks))
				return;
			evtAdded.remove(addedListener);
			evtRemoved.remove(removedListener);
		}
		finally {
			writeLock.unlock();
		}
		
		for (Task task : removedTasks)
			evtRemoved.fire(task);
	}
	
	// returns 'proceed', fills the given list with removed tasks
	@NotThreadSafe
	private boolean removeAll(	@NotNull CancelHandler handler,
								@NotNull LazyList<Task> removedTasks) {
		/*
		 * Cancel active task if there is one. Note that if the cancel
		 * handler returns null, no tasks are removed.
		 */
		for (Task task : tasks) {
			if (!task.is(TaskState.INDEXING))
				continue;
			if (task.is(IndexAction.UPDATE)) {
				task.cancelAction = CancelAction.KEEP;
			}
			else {
				task.cancelAction = handler.cancel();
				if (task.cancelAction == null)
					return false;
			}
		}

		// Remove all tasks (including active task)
		removedTasks.addAll(tasks);
		tasks.clear();
		return true;
	}

	// Event listeners are notified without lock and possibly from a non-GUI
	// thread. Handler is called without lock and in the same thread as the caller of this method.
	// The list of tasks given to the handler is an immutable copy
	@ThreadSafe
	public void addListeners(	@NotNull ExistingTasksHandler handler,
								@NotNull Event.Listener<Task> addedListener,
								@NotNull Event.Listener<Task> removedListener) {
		Util.checkNotNull(handler, addedListener, removedListener);
		ImmutableList<Task> tasksCopy;
		writeLock.lock();
		try {
			tasksCopy = ImmutableList.copyOf(tasks);
			evtAdded.add(addedListener);
			evtRemoved.add(removedListener);
		}
		finally {
			writeLock.unlock();
		}
		handler.handleExistingTasks(tasksCopy);
	}

	public void removeListeners(@NotNull Event.Listener<Task> addedListener,
								@NotNull Event.Listener<Task> removedListener) {
		Util.checkNotNull(addedListener, removedListener);
		writeLock.lock();
		try {
			evtAdded.remove(addedListener);
			evtRemoved.remove(removedListener);
		}
		finally {
			writeLock.unlock();
		}
	}
	
	@ThreadSafe
	void remove(@NotNull Task task, @NotNull CancelHandler handler) {
		Util.checkNotNull(task, handler);
		boolean fireRemoved = false;
		
		writeLock.lock();
		try {
			if (task.is(TaskState.INDEXING)) {
				if (task.is(IndexAction.UPDATE)) {
					task.cancelAction = CancelAction.KEEP;
				}
				else {
					task.cancelAction = handler.cancel(); // May return null
					if (task.cancelAction == null)
						return;
				}
			}
			fireRemoved = tasks.remove(task);
		}
		finally {
			writeLock.unlock();
		}
		
		if (fireRemoved)
			evtRemoved.fire(task);
	}
	
	@ThreadSafe
	void setReady(@NotNull Task task) {
		Util.checkNotNull(task);
		LazyList<Task> removedTasks = new LazyList<Task>();
		
		writeLock.lock();
		try {
			Util.checkThat(task.cancelAction == null);
			if (!task.is(TaskState.NOT_READY))
				return;
			task.set(TaskState.READY);

			// Remove redundant update tasks from the queue
			Iterator<Task> it = tasks.iterator();
			while (it.hasNext()) {
				Task queueTask = it.next();
				if (queueTask != task && queueTask.is(IndexAction.UPDATE)
						&& queueTask.is(TaskState.READY)
						&& IndexingQueue.sameTarget(queueTask, task)) {
					it.remove();
					removedTasks.add(queueTask);
				}
			}

			readyTaskAvailable.signal();
		}
		finally {
			writeLock.unlock();
		}
		
		for (Task removedTask : removedTasks)
			evtRemoved.fire(removedTask);
	}
	
	// should only be called with indexes that are currently in the registry
	// approves immediately if no matching task is found
	// if matching inactive task is found, approval is given after removing the
	// task from the queue
	// if matching active task is found, the task is canceled, and the approval
	// is given after the indexing operation has finished
	@ThreadSafe
	@VisibleForPackageGroup
	public void approveDeletions(@NotNull List<PendingDeletion> deletions) {
		Util.checkNotNull(deletions);
		if (deletions.isEmpty())
			return;
		
		LazyList<Task> removedTasks = new LazyList<Task>();
		
		writeLock.lock();
		try {
			for (PendingDeletion deletion : deletions) {
				Iterator<Task> it = tasks.iterator();
				boolean approveImmediately = true;
				while (it.hasNext()) {
					Task task = it.next();
					if (task.getLuceneIndex() != deletion.getLuceneIndex())
						continue;
					if (task.is(TaskState.INDEXING)) {
						Util.checkThat(task.is(IndexAction.UPDATE));
						approveImmediately = false;
						task.setDeletion(deletion);
						task.cancelAction = CancelAction.KEEP;
					}
					/*
					 * Remove the task from the queue even if it was in indexing
					 * state - we don't want any ghost tasks to linger in the
					 * queue, since they might (theoretically) cause subsequent
					 * task addition request to fail due to directory overlaps.
					 */
					it.remove();
					removedTasks.add(task);
				}
				if (approveImmediately)
					deletion.setApprovedByQueue();
			}
		}
		finally {
			writeLock.unlock();
		}
		
		for (Task task : removedTasks)
			evtRemoved.fire(task);
	}

	// returns 'proceed'; cancel handler is called if a creation or rebuild
	// task is currently running
	// Warning: Cancel handler is called under lock, so caller must take possible
	// lock-ordering deadlocks into account.
	/*
	 * This method must not be called again after a previous call has set the
	 * shutdown flag.
	 */
	@ThreadSafe
	public boolean shutdown(@NotNull final CancelHandler handler) {
		Util.checkNotNull(handler);
		LazyList<Task> removedTasks = new LazyList<Task>();
		
		writeLock.lock();
		try {
			if (shutdown)
				throw new UnsupportedOperationException();
			if (!removeAll(handler, removedTasks))
				return false;
			shutdown = true;
			
			/*
			 * Wake up and terminate worker thread if it was waiting. Do *not*
			 * call thread.interrupt here, otherwise we'll get an exception when
			 * trying to close the current Lucene index, if there is one.
			 */
			readyTaskAvailable.signal();
		}
		finally {
			writeLock.unlock();
		}
		
		for (Task task : removedTasks)
			evtRemoved.fire(task);

		return true;
	}

}
