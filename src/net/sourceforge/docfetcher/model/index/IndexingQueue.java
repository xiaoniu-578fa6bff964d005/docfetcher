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
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.ImmutableList;

import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.NotThreadSafe;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.ThreadSafe;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.index.Task.TaskState;
import net.sourceforge.docfetcher.model.index.file.FileIndex;
import net.sourceforge.docfetcher.model.index.outlook.OutlookIndex;

/**
 * @author Tran Nam Quang
 */
public final class IndexingQueue {

	public interface ExistingTasksHandler {
		public void handleExistingTasks(@NotNull List<Task> tasks);
	}

	public enum Rejection {
		OVERLAP_WITH_REGISTRY,
		OVERLAP_WITH_QUEUE,
		SAME_IN_REGISTRY,
		SAME_IN_QUEUE,
		REDUNDANT_UPDATE,
		SHUTDOWN,
	}

	private final Event<Task> evtAdded = new Event<Task>();
	private final Event<Task> evtRemoved = new Event<Task>();

	private final Thread thread;
	private final IndexRegistry indexRegistry;
	private final LinkedList<Task> tasks = new LinkedList<Task>();

	private volatile boolean shutdown = false;
	final Lock lock = new ReentrantLock();
	final Condition readyTaskAvailable = lock.newCondition();
	final int reporterCapacity;

	public IndexingQueue(	@NotNull final IndexRegistry indexRegistry,
							int reporterCapacity) {
		this.indexRegistry = indexRegistry;
		this.reporterCapacity = reporterCapacity;
		
		/*
		 * In case of rebuild tasks, if a task is removed before it has entered
		 * the indexing state, put the associated index back into the registry.
		 * If the task has already entered the indexing state, putting the index
		 * back into the registry is the responsibility of the worker thread.
		 */
		evtRemoved.add(new Event.Listener<Task>() {
			public void update(Task task) {
				if (!task.is(IndexAction.REBUILD))
					return;
				if (!task.is(TaskState.NOT_READY) && !task.is(TaskState.READY))
					return;
				LuceneIndex luceneIndex = task.getLuceneIndex();
				assert !indexRegistry.getIndexes().contains(luceneIndex);
				indexRegistry.addIndex(luceneIndex);
				indexRegistry.save(luceneIndex);
			}
		});
		
		thread = new Thread(IndexingQueue.class.getName()) {
			public void run() {
				while (true) {
					// Wait for next task
					Task task;
					lock.lock();
					try {
						task = getReadyTask();
						while (task == null) {
							readyTaskAvailable.await();
							task = getReadyTask();
						}
					}
					catch (InterruptedException e) {
						// Program shutdown; get out of the loop
						break;
					}
					finally {
						lock.unlock();
					}

					assert isValidRegistryState(indexRegistry, task);

					// Indexing
					task.set(TaskState.INDEXING);
					LuceneIndex luceneIndex = task.getLuceneIndex();
					if (task.is(IndexAction.REBUILD))
						luceneIndex.clear();
					boolean success = task.update(); // Long-running process

					// Post-processing
					lock.lock();
					try {
						if (task.is(IndexAction.UPDATE)) {
							/*
							 * In case of index updates, save to disk even if
							 * the indexing fails. An alternative is to
							 * automatically discard the index, which would
							 * probably confuse and/or annoy the user, because
							 * then indexes could magically disappear at any
							 * time.
							 */
							assert !task.is(CancelAction.DISCARD);
							indexRegistry.save(luceneIndex);
							remove(task);
						}
						else if (!success) {
							luceneIndex.delete();
						}
						else if (task.is(CancelAction.DISCARD)) {
							luceneIndex.delete();
							remove(task);
						}
						else {
							indexRegistry.addIndex(luceneIndex);
							indexRegistry.save(luceneIndex);
							boolean keep = task.is(CancelAction.KEEP);
							boolean noErrors = true; // TODO
							if (keep || noErrors || shutdown)
								remove(task);
						}
						task.set(TaskState.FINISHED);
					}
					finally {
						lock.unlock();
					}
				}
			}
		};
		thread.start();
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
		File taskIndexDir = task.getLuceneIndex().getIndexDir();
		File taskParentIndexDir = Util.getParentFile(taskIndexDir);
		File indexParentDir = indexRegistry.getIndexParentDir();
		Util.checkThat(Util.equals(taskParentIndexDir, indexParentDir));

		lock.lock();
		try {
			assert task.cancelAction == null;
			assert task.is(TaskState.NOT_READY) || task.is(TaskState.READY);
			if (shutdown)
				return Rejection.SHUTDOWN;

			if (task.is(IndexAction.UPDATE)) {
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
			else if (task.getLuceneIndex() instanceof OutlookIndex) {
				/*
				 * Reject a request to create or rebuild an Outlook index for a
				 * certain PST file if the latter was already indexed or is
				 * already lined up in the queue.
				 */
				for (LuceneIndex index0 : indexRegistry.getIndexes())
					if (index0 instanceof OutlookIndex
							&& sameTarget(index0, task))
						return Rejection.SAME_IN_REGISTRY;
				for (Task queueTask : tasks) {
					assert queueTask != task;
					if (queueTask.getLuceneIndex() instanceof OutlookIndex
							&& sameTarget(queueTask, task))
						return Rejection.SAME_IN_QUEUE;
				}
			}
			else {
				/*
				 * Reject a request to create or rebuild a file index if it
				 * overlaps with a file index in the registry or in the queue.
				 */
				assert task.getLuceneIndex() instanceof FileIndex;
				for (LuceneIndex index0 : indexRegistry.getIndexes()) {
					if (index0 instanceof OutlookIndex)
						continue;
					File f1 = index0.getRootFile();
					File f2 = task.getLuceneIndex().getRootFile();
					if (Util.equals(f1, f2))
						return Rejection.SAME_IN_REGISTRY;
					if (isOverlapping(f1, f2))
						return Rejection.OVERLAP_WITH_REGISTRY;
				}
				for (Task queueTask : tasks) {
					assert queueTask != task;
					if (queueTask.getLuceneIndex() instanceof OutlookIndex)
						continue;
					File f1 = queueTask.getLuceneIndex().getRootFile();
					File f2 = task.getLuceneIndex().getRootFile();
					if (Util.equals(f1, f2))
						return Rejection.SAME_IN_QUEUE;
					if (isOverlapping(f1, f2))
						return Rejection.OVERLAP_WITH_QUEUE;
				}
			}

			tasks.add(task);
			evtAdded.fire(task);
			if (task.is(TaskState.READY))
				readyTaskAvailable.signal();
			return null;
		}
		finally {
			lock.unlock();
		}
	}

	@NotThreadSafe
	static boolean sameTarget(@NotNull Task task1, @NotNull Task task2) {
		File target1 = task1.getLuceneIndex().getRootFile();
		File target2 = task2.getLuceneIndex().getRootFile();
		return Util.equals(target1, target2);
	}

	@NotThreadSafe
	private static boolean sameTarget(	@NotNull LuceneIndex index,
										@NotNull Task task) {
		File target1 = index.getRootFile();
		File target2 = task.getLuceneIndex().getRootFile();
		return Util.equals(target1, target2);
	}

	@NotThreadSafe
	private static boolean isOverlapping(@NotNull File f1, @NotNull File f2) {
		return Util.contains(f1, f2) || Util.contains(f2, f1);
	}

	// see Task.remove(CancelHandler)
	@ThreadSafe
	public void removeAll(	@NotNull CancelHandler handler,
							@NotNull Event.Listener<Task> addedListener,
							@NotNull Event.Listener<Task> removedListener) {
		Util.checkNotNull(handler, addedListener, removedListener);
		lock.lock();
		try {
			Iterator<Task> it = tasks.iterator();
			while (it.hasNext()) {
				Task task = it.next();
				if (task.is(TaskState.INDEXING)) {
					if (task.is(IndexAction.UPDATE))
						task.cancelAction = CancelAction.KEEP;
					else
						task.cancelAction = handler.cancel(); // May return null
				}
				else {
					it.remove();
					evtRemoved.fire(task);
				}
			}

			// Must be done *after* firing the removed events
			evtAdded.remove(addedListener);
			evtRemoved.remove(removedListener);
		}
		finally {
			lock.unlock();
		}
	}

	// Event listeners are notified under lock and possibly from a non-GUI
	// thread
	// The list of tasks given to the handler is an immutable copy
	@ThreadSafe
	public void addListeners(	@NotNull ExistingTasksHandler handler,
								@NotNull Event.Listener<Task> addedListener,
								@NotNull Event.Listener<Task> removedListener) {
		Util.checkNotNull(handler, addedListener, removedListener);
		lock.lock();
		try {
			handler.handleExistingTasks(ImmutableList.copyOf(tasks));
			evtAdded.add(addedListener);
			evtRemoved.add(removedListener);
		}
		finally {
			lock.unlock();
		}
	}

	public void removeListeners(@NotNull Event.Listener<Task> addedListener,
								@NotNull Event.Listener<Task> removedListener) {
		Util.checkNotNull(addedListener);
		lock.lock();
		try {
			evtAdded.remove(addedListener);
			evtRemoved.remove(removedListener);
		}
		finally {
			lock.unlock();
		}
	}

	@NotThreadSafe
	void remove(@NotNull Task task) {
		Util.checkNotNull(task);
		boolean removed = tasks.remove(task);
		assert removed;
		evtRemoved.fire(task);
	}

	// Iterator supports removal of elements
	@NotNull
	@NotThreadSafe
	Iterator<Task> iterator() {
		final Iterator<Task> innerIterator = tasks.iterator();
		return new Iterator<Task>() {
			private Task lastElement;

			public boolean hasNext() {
				return innerIterator.hasNext();
			}

			public Task next() {
				return lastElement = innerIterator.next();
			}

			public void remove() {
				innerIterator.remove();
				evtRemoved.fire(lastElement);
			}
		};
	}

	// TODO call shutdown
	// returns 'proceed'; cancel handler is called if a creation or rebuild
	// task is currently running
	/*
	 * This method must not be called again after a previous call has set the
	 * shutdown flag.
	 */
	@ThreadSafe
	public boolean shutdown(@NotNull final CancelHandler handler) {
		if (shutdown)
			throw new UnsupportedOperationException();

		lock.lock();
		try {
			// Cancel active task if there is one
			final boolean[] doShutdown = { true };
			for (Task task : tasks) {
				if (!task.is(TaskState.INDEXING))
					continue;
				task.remove(new CancelHandler() {
					public CancelAction cancel() {
						CancelAction action = handler.cancel();
						if (action == null)
							doShutdown[0] = false;
						return action;
					}
				});
				break; // There should be only one active task at any time
			}

			if (!doShutdown[0])
				return false;

			// Cancel all inactive tasks
			Iterator<Task> it = tasks.iterator();
			while (it.hasNext()) {
				Task task = it.next();
				if (task.is(TaskState.INDEXING))
					continue;
				it.remove();
				evtRemoved.fire(task);
			}

			shutdown = true;
		}
		finally {
			lock.unlock();
		}

		// Wake up and terminate worker thread if it was waiting
		thread.interrupt();

		return true;
	}

}
