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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Utility class for creating a drop-down menu located at the bottom of a given
 * tool item, and having a given parent control. The latter is usually the
 * control containing the tool bar.
 */
public final class DropDownMenuManager {
	
	private final List<MenuAction> menuItems = new ArrayList<MenuAction>();
	private final ToolItem toolItem;
	private final Point location;
	private final Control parent;
	
	public DropDownMenuManager(	@NotNull ToolItem toolItem,
								@NotNull Control parent) {
		Util.checkNotNull(toolItem, parent);
		this.toolItem = toolItem;
		this.parent = parent;
		location = null;
	}
	
	public DropDownMenuManager(@NotNull Point location, @NotNull Control parent) {
		Util.checkNotNull(location, parent);
		this.location = location;
		this.parent = parent;
		toolItem = null;
	}
	
	@NotNull
	public Menu show() {
		// Create menu and set its location
		final Menu menu = new Menu(parent);
		if (location == null) {
			Rectangle bounds = toolItem.getBounds();
			bounds.y += bounds.height;
			bounds = parent.getDisplay().map(toolItem.getParent(), null, bounds);
			menu.setLocation(bounds.x, bounds.y);
		}
		else {
			menu.setLocation(location.x, location.y);
		}
		
		// Create menu items from stored menu actions
		for (final MenuAction action : menuItems) {
			// Create separator item
			if (action == null) {
				new MenuItem(menu, SWT.SEPARATOR);
				continue;
			}
			
			// Create regular menu item
			MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setImage(action.getImage());
			menuItem.setText(action.getLabel());
			menuItem.setEnabled(action.isEnabled());
			menuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					action.run();
				}
			});
		}
		
		// Dispose of menu after it is hidden
		menu.addMenuListener(new MenuAdapter() {
			public void menuHidden(MenuEvent e) {
				Util.runAsyncExec(parent, new Runnable() {
					public void run() {
						/*
						 * This must be run in an asyncExec, otherwise we'll
						 * dispose the menu before the menu actions can be
						 * executed.
						 */
						menu.dispose();
					}
				});
			}
		});
		
		menu.setVisible(true);
		return menu;
	}
	
	public void add(@NotNull MenuAction action) {
		Util.checkNotNull(action);
		menuItems.add(action);
	}
	
	public void addSeparator() {
		menuItems.add(null);
	}

}
