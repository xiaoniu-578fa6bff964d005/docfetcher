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

package net.sourceforge.docfetcher.util.gui;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public final class ContextMenuManager {
	
	public static void main(String[] args) {
		final Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(Util.createFillLayout(5));
		Util.setCenteredBounds(shell, 400, 300);
		
		final int timeout = 2000;
		
		Label label = new Label(shell, SWT.CENTER);
		label.setText("Right-click on the area below to open the menu.\n" +
				String.format("(It will automatically close after %d ms.)", timeout));
		
		class MyMenuAction extends MenuAction {
			private final String label;
			private final boolean enabled;
			public MyMenuAction(String label, boolean enabled) {
				super(label);
				this.label = label;
				this.enabled = enabled;
			}
			public boolean isEnabled() {
				return enabled;
			}
			public void run() {
				Util.println(label + " clicked");
			}
		}
		
		ContextMenuManager menuManager = new ContextMenuManager(label);
		
		menuManager.add(new MyMenuAction("item 1", true));
		menuManager.add(new MyMenuAction("item 2", false));
		menuManager.addSeparator();
		
		Menu submenu = menuManager.addSubmenu(new MenuAction("submenu"));
		menuManager.add(submenu, new MyMenuAction("sub-item 1", true));
		menuManager.add(submenu, new MyMenuAction("sub-item 2", false));
		menuManager.addSeparator(submenu);
		
		final Menu menu = label.getMenu();
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				display.timerExec(timeout, new Runnable() {
					public void run() {
						menu.setVisible(false);
					}
				});
			}
		});

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	private final Menu menu;

	public ContextMenuManager(@NotNull Control control) {
		Util.checkNotNull(control);
		menu = new Menu(control);
		control.setMenu(menu);
		control.addMenuDetectListener(new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent e) {
				updateEnabledStates(menu);
			}
		});
	}
	
	@RecursiveMethod
	private static void updateEnabledStates(@NotNull Menu menu) {
		for (MenuItem item : menu.getItems()) {
			MenuAction action = (MenuAction) item.getData();
			if (action == null) {
				assert Util.contains(item.getStyle(), SWT.SEPARATOR);
				continue;
			}
			item.setEnabled(action.isEnabled());
			if (Util.contains(item.getStyle(), SWT.CASCADE))
				updateEnabledStates(item.getMenu());
		}
	}

	public void add(@NotNull MenuAction action) {
		add(menu, action);
	}
	
	public void add(@NotNull Menu submenu, @NotNull final MenuAction action) {
		Util.checkNotNull(action);
		MenuItem item = new MenuItem(submenu, SWT.PUSH);
		item.setImage(action.getImage());
		item.setText(action.getLabel());
		item.setEnabled(action.isEnabled());
		item.setData(action);
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				 * The conditions required to execute the action might have
				 * changed while the context menu was displayed, so we should
				 * double-check before executing the action.
				 */
				if (action.isEnabled())
					action.run();
			}
		});
	}
	
	public void addSeparator() {
		new MenuItem(menu, SWT.SEPARATOR);
	}
	
	public void addSeparator(@NotNull Menu submenu) {
		new MenuItem(submenu, SWT.SEPARATOR);
	}
	
	// action will not be executed
	@NotNull
	public Menu addSubmenu(@NotNull MenuAction action) {
		Util.checkNotNull(action);
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setImage(action.getImage());
		item.setText(action.getLabel());
		item.setEnabled(action.isEnabled());
		item.setData(action);
		Menu submenu = new Menu(item);
		item.setMenu(submenu);
		return submenu;
	}

}
