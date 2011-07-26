/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.index;

import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public abstract class Stoppable <T extends Throwable> {
	
	private boolean stopped = false;
	@Nullable private T t;
	
	/**
	 * Returns whether a stop signal was sent.
	 */
	public final boolean isStopped() {
		return stopped;
	}
	
	/**
	 * Sends a stop signal to the running process.
	 */
	public final void stop() {
		stopped = true;
	}
	
	/**
	 * Sends a stop signal to the running process and throws the given Throwable
	 * after the process has stopped. If this method is called multiple times,
	 * each call will replace the last Throwable with the new one. Note that the
	 * Throwable argument can be null, in which case no Throwable will be
	 * thrown.
	 */
	public final void stop(@Nullable T t) {
		stopped = true;
		this.t = t;
	}
	
	/**
	 * Starts the process. If a non-null Throwable was set via
	 * {@link #stop(Throwable)} during the execution of the process, this method
	 * will throw the Throwable.
	 */
	public final void run() throws T {
		try {
			doRun();
		} finally {
			runFinally();
		}
		if (t != null) throw t;
	}
	
	/**
	 * Starts the process. Does not throw any Throwables, even if one was set
	 * via {@link #stop(Throwable)}.
	 */
	public final void runSilently() {
		doRun();
		runFinally();
	}
	
	// subclasser should check isStopped at appropriate times
	protected abstract void doRun();
	
	// cleanup code to run even if an exception was thrown
	protected void runFinally() {}
	
}
