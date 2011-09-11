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
public final class BlockingWrapper<T> {
	
	private final Lock lock = new ReentrantLock();
	private final Condition notNull = lock.newCondition();
	
	@Nullable private T object; // guarded by lock
	
	// Blocks until the wrapped object is not null
	// May return null if the calling thread was interrupted
	@Nullable
	public T get() {
		lock.lock();
		try {
			while (object == null) {
				try {
					notNull.await();
				}
				catch (InterruptedException e) {
					return null;
				}
			}
			return object;
		}
		finally {
			lock.unlock();
		}
	}
	
	public void set(@NotNull T object) {
		Util.checkNotNull(object);
		lock.lock();
		try {
			boolean wasNull = this.object == null;
			this.object = object;
			if (wasNull)
				notNull.signal();
		}
		finally {
			lock.unlock();
		}
	}
	
	public boolean isNull() {
		lock.lock();
		try {
			return object == null;
		}
		finally {
			lock.unlock();
		}
	}

}
