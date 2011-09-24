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

package net.sourceforge.docfetcher.gui.indexing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.NotThreadSafe;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
public final class ProgressPanel {

	public static void main(String[] args) {
		Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Util.setCenteredBounds(shell, 400, 300);

		final ProgressPanel progressPanel = new ProgressPanel(shell, 1000);

		new Thread() {
			public void run() {
				int i = 0;
				while (!shell.isDisposed()) {
					progressPanel.append(i + "");
					i++;
//					try {
//						Thread.sleep(2000);
//					}
//					catch (InterruptedException e) {
//						return;
//					}
				}
			}
		}.start();

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	private final Lock lock = new ReentrantLock(true);
	private final Condition notEmpty = lock.newCondition();
	
	private final LinkedList<String> queue = new LinkedList<String>(); // guarded by lock
	private boolean replaceLastInTable = false; // guarded by lock
	
	private final int itemLimit;
	private final Table table;
	private final LinkedList<String> tableItems = new LinkedList<String>(); // accessed exclusively by worker thread

	public ProgressPanel(@NotNull Composite parent, final int itemLimit) {
		Util.checkThat(itemLimit >= 2);
		this.itemLimit = itemLimit;
		table = new Table(parent, SWT.V_SCROLL | SWT.BORDER | SWT.VIRTUAL);
		
		table.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				TableItem item = (TableItem) event.item;
				item.setText(tableItems.get(event.index));
			}
		});

		/*
		 * The approach taken here is to put the incoming messages in a queue
		 * for bulk processing in regular intervals, rather than to process each
		 * incoming message immediately, as it was done in DocFetcher 1.0.3 and
		 * earlier versions.
		 * 
		 * Tests indicate that the bulk approach leads to a significant
		 * performance increase when a large number of small files (usually
		 * plain text files) is processed. In such cases, the queue size can go
		 * up to 30-70 items before the worker thread takes another round. Thus,
		 * many unnecessary GUI updates are avoided, which would otherwise have
		 * slowed down the indexing.
		 * 
		 * A typical scenario where a large number of small files has to be
		 * processed is a large codebase consisting of thousands of source code
		 * files.
		 */
		
		final Thread thread = new Thread(ProgressPanel.class.getName()) {
			public void run() {
				while (true) {
					List<String> subList;
					final boolean _replaceLastInTable;
					lock.lock();
					try {
						while (queue.isEmpty())
							notEmpty.await();
						int size = queue.size();
						int start = Math.max(0, size - itemLimit);
						subList = queue.subList(start, size);
						subList = new ArrayList<String>(subList);
						queue.clear();
						
						_replaceLastInTable = replaceLastInTable;
						replaceLastInTable = false;
					}
					catch (InterruptedException e) {
						break;
					}
					finally {
						lock.unlock();
					}
					
					// Display messages; should be done without holding the lock
					append(subList, _replaceLastInTable);
					
					try {
						/*
						 * Sleeping for 40 ms gives us a frame rate of 1000/40 =
						 * 25 fps, which is known to be the minimum frame rate
						 * to make animations look "smooth".
						 */
						Thread.sleep(40);
					}
					catch (InterruptedException e) {
						break;
					}
				}
			}
		};
		thread.start();
		
		table.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				thread.interrupt();
			}
		});
	}
	
	@NotNull
	public Control getControl() {
		return table;
	}
	
	@ThreadSafe
	public void append(@NotNull String message) {
		Util.checkNotNull(message);
		lock.lock();
		try {
			queue.add(message);
			notEmpty.signal();
		}
		finally {
			lock.unlock();
		}
	}
	
	@ThreadSafe
	public void replaceLast(@NotNull String message) {
		Util.checkNotNull(message);
		lock.lock();
		try {
			if (queue.isEmpty()) {
				assert !replaceLastInTable;
				replaceLastInTable = true;
			}
			else {
				queue.removeLast();
			}
			queue.add(message);
			notEmpty.signal();
		}
		finally {
			lock.unlock();
		}
	}

	@NotThreadSafe
	private void append(@NotNull final List<String> messages,
						final boolean replaceLastInTable) {
		Util.checkThat(!messages.isEmpty());
		Util.checkThat(messages.size() <= itemLimit);
		Util.runSyncExec(table, new Runnable() {
			public void run() {
				table.setRedraw(false);
				
				int replaceIndex = -1;
				if (replaceLastInTable && !tableItems.isEmpty()) {
					tableItems.removeLast();
					replaceIndex = tableItems.size();
				}
				
				int newItemCount = tableItems.size() + messages.size();
				int removeCount = newItemCount - itemLimit;
				
				if (removeCount > 0) {
					while (removeCount > 0) {
						tableItems.removeFirst();
						removeCount--;
					}
					tableItems.addAll(messages);
					tableItems.set(0, "...");
					table.clearAll();
					table.setItemCount(itemLimit);
				}
				else {
					tableItems.addAll(messages);
					if (replaceIndex != -1)
						table.clear(replaceIndex);
					table.setItemCount(newItemCount);
				}
				
				TableItem lastItem = table.getItem(tableItems.size() - 1);
				table.showItem(lastItem);
				
				table.setRedraw(true);
			}
		});
	}

}
