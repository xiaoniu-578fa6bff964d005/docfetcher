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

package net.sourceforge.docfetcher.base.gui;

import net.sourceforge.docfetcher.base.Util;

import org.eclipse.swt.widgets.Display;


public class StatusManager {
	
	public static interface StatusWidgetProvider {
		public String getStatus();
		public void setStatus(String text);
	}
	
	private Display display;
	private StatusWidgetProvider statusLine;
	private volatile Thread thread;
	
	public StatusManager(Display display, StatusWidgetProvider statusWidget) {
		this.display = display;
		this.statusLine = statusWidget;
	}
	
	/**
	 * Sets the message to be displayed in the status bar for the given duration
	 * in milliseconds. If the latter is <= 0, the message is shown without time
	 * limit.
	 */
	public void setStatus(final String message, final int milliseconds) {
		// Stop previous thread so it won't interfere with us
		if (thread != null)
			thread.interrupt();
		
		// Set status
		boolean wasRun = Util.runSyncExec(display, new Runnable() {
			public void run() {
				statusLine.setStatus(message);
			}
		});
		if (!wasRun) return;
		
		if (milliseconds <= 0) return;
		
		// Clear status after delay
		thread = new Thread(StatusManager.class.getName() + " (clear status)") {
			public void run() {
				try {
					Thread.sleep(milliseconds);
					Util.runSyncExec(display, new Runnable() {
						public void run() {
							/*
							 * Clear status line, unless someone else has
							 * changed it.
							 */
							String status = statusLine.getStatus();
							if (status.equals(message))
								statusLine.setStatus("");
						}
					});
				} catch (InterruptedException e) {
				}
				thread = null;
			}
		};
		thread.start();
	}

}
