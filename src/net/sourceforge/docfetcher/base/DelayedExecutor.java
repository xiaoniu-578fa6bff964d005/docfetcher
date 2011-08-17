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

package net.sourceforge.docfetcher.base;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
public final class DelayedExecutor {
	
	private final ScheduledExecutorService executor;
	private final int delay;
	private final Object lock = new Object();
	
	@Nullable private ScheduledFuture<?> lastFuture; // guarded by lock
	@Nullable private Object lastId; // guarded by lock

	public DelayedExecutor(int delay) {
		Util.checkThat(delay >= 0);
		this.delay = delay;
		executor = Executors.newScheduledThreadPool(1);
	}
	
	public void schedule(@NotNull final Runnable runnable) {
		Util.checkNotNull(runnable);
		synchronized (lock) {
			if (lastFuture != null) {
				assert lastId != null;
				lastFuture.cancel(false);
			}
			final Object id = new Object();
			lastFuture = executor.schedule(new Runnable() {
				public void run() {
					runnable.run();
					synchronized (lock) {
						if (id == lastId) {
							lastId = null;
							lastFuture = null;
						}
					}
				}
			}, delay, TimeUnit.MILLISECONDS);
			lastId = id;
		}
	}
	
	public void shutdown() {
		executor.shutdown();
	}

}
