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

package net.sourceforge.docfetcher.util.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
public abstract class MergingBlockingQueue<T> {
	
	@Nullable private T item; // guarded by lock
	private final Lock lock = new ReentrantLock(true);
	private final Condition notNull = lock.newCondition();
	
	public void put(@NotNull T item) {
		Util.checkNotNull(item);
		lock.lock();
		try {
			if (this.item == null)
				this.item = item;
			else
				this.item = merge(this.item, item);
			notNull.signal();
		}
		finally {
			lock.unlock();
		}
	}
	
	@NotNull
	public T take() throws InterruptedException {
		lock.lock();
		try {
			while (item == null)
				notNull.await();
			T ret = item;
			item = null;
			return ret;
		}
		finally {
			lock.unlock();
		}
	}
	
	@NotNull
	protected abstract T merge(@NotNull T item1, @NotNull T item2);

}
