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

import java.util.Iterator;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.ThreadSafe;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.PendingDeletion;
import net.sourceforge.docfetcher.model.index.DelegatingReporter.ExistingMessagesHandler;
import net.sourceforge.docfetcher.model.index.DelegatingReporter.ExistingMessagesProvider;

import com.google.common.base.Objects;

/**
 * @author Tran Nam Quang
 */
public final class Task {

	public interface CancelHandler {
		/**
		 * Returns the type of cancelation. May be null to indicate that the
		 * indexing should not be canceled.
		 */
		@Nullable
		public CancelAction cancel();
	}

	public enum IndexAction {
		CREATE, UPDATE, REBUILD
	}

	public enum CancelAction {
		DISCARD, KEEP
	}

	public enum TaskState {
		NOT_READY, READY, INDEXING, FINISHED
	}

	private final IndexingQueue queue;
	private final LuceneIndex index;
	private final IndexAction indexAction;
	@NotNull private volatile TaskState state;
	@Nullable private volatile PendingDeletion deletion;
	private final DelegatingReporter reporter;
	@Nullable volatile CancelAction cancelAction;

	Task(	@NotNull IndexingQueue queue,
			@NotNull LuceneIndex index,
			@NotNull IndexAction indexAction) {
		Util.checkNotNull(queue, index, indexAction);
		this.queue = queue;
		this.index = index;
		this.indexAction = indexAction;
		state = is(IndexAction.UPDATE) ? TaskState.READY : TaskState.NOT_READY;
		reporter = new DelegatingReporter(queue.reporterCapacity);
	}

	// If the task is not in indexing state, the task is simply removed.
	// If it is in indexing state, the handler is called for 'Create' and
	// 'Rebuild' jobs to determine whether to discard or keep the index. In case
	// of 'Update' tasks, the index is always kept. If in indexing state, only
	// a cancel flag is set, but the task may be removed later.
	// See IndexingQueue.removeAll(CancelHandler)
	@ThreadSafe
	public void remove(@NotNull CancelHandler handler) {
		Util.checkNotNull(handler);
		queue.lock.lock();
		try {
			if (is(TaskState.INDEXING)) {
				if (is(IndexAction.UPDATE))
					cancelAction = CancelAction.KEEP;
				else
					cancelAction = handler.cancel(); // May return null
			}
			queue.remove(this);
		}
		finally {
			queue.lock.unlock();
		}
	}

	// for index updates, the ready-state is set automatically
	@ThreadSafe
	public void setReady() {
		queue.lock.lock();
		try {
			Util.checkThat(cancelAction == null);
			if (!is(TaskState.NOT_READY))
				return;
			set(TaskState.READY);

			// Remove redundant update tasks from the queue
			Iterator<Task> it = queue.iterator();
			while (it.hasNext()) {
				Task queueTask = it.next();
				if (queueTask != this && queueTask.is(IndexAction.UPDATE)
						&& queueTask.is(TaskState.READY)
						&& IndexingQueue.sameTarget(queueTask, this))
					it.remove();
			}

			queue.readyTaskAvailable.signal();
		}
		finally {
			queue.lock.unlock();
		}
	}

	boolean update() {
		return index.update(reporter, new Cancelable() {
			public boolean isCanceled() {
				return cancelAction != null;
			}
		});
	}

	@NotNull
	public LuceneIndex getLuceneIndex() {
		return index;
	}

	public void attachReporter(	@NotNull IndexingReporter delegate,
								@NotNull ExistingMessagesHandler handler) {
		reporter.attachDelegate(delegate, handler);
	}

	public void detachReporter(	@NotNull IndexingReporter delegate,
								@NotNull ExistingMessagesProvider provider) {
		reporter.detachDelegate(delegate, provider);
	}

	@ThreadSafe
	public boolean is(@Nullable IndexAction indexAction) {
		// No lock needed because indexAction is immutable
		return Objects.equal(this.indexAction, indexAction);
	}

	@ThreadSafe
	boolean is(@Nullable CancelAction cancelAction) {
		queue.lock.lock();
		try {
			return Objects.equal(this.cancelAction, cancelAction);
		}
		finally {
			queue.lock.unlock();
		}
	}

	@ThreadSafe
	public boolean is(@Nullable TaskState state) {
		queue.lock.lock();
		try {
			return Objects.equal(this.state, state);
		}
		finally {
			queue.lock.unlock();
		}
	}

	@ThreadSafe
	void set(@NotNull TaskState state) {
		queue.lock.lock();
		try {
			this.state = Util.checkNotNull(state);
		}
		finally {
			queue.lock.unlock();
		}
	}
	
	@ThreadSafe
	void setDeletion(@NotNull PendingDeletion deletion) {
		queue.lock.lock();
		try {
			this.deletion = Util.checkNotNull(deletion);
		}
		finally {
			queue.lock.unlock();
		}
	}
	
	@Nullable
	@ThreadSafe
	PendingDeletion getDeletion() {
		queue.lock.lock();
		try {
			return deletion;
		}
		finally {
			queue.lock.unlock();
		}
	}

}