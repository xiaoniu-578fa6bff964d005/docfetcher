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

package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

/**
 * @author Tran Nam Quang
 */
public final class SystemTrayHider {
	
	public final Event<Void> evtHiding = new Event<Void>();
	public final Event<Void> evtRestored = new Event<Void>();
	public final Event<Void> evtShutdown = new Event<Void>();
	
	private final Shell shell;
	@Nullable private TrayItem trayItem;
	@Nullable private Point shellLocation;

	public SystemTrayHider(@NotNull Shell shell) {
		Util.checkNotNull(shell);
		this.shell = shell;
	}
	
	/**
	 * Hides the shell in the system tray.
	 */
	public void hide() {
		if (!shell.isVisible())
			return;
		
		Tray tray = shell.getDisplay().getSystemTray();
		if (tray == null) {
			AppUtil.showError(Msg.systray_not_available.get(), true, false);
			return;
		}
		
		// Create and configure tray item
		trayItem = new TrayItem (tray, SWT.NONE);
		trayItem.setToolTipText(ProgramConf.Str.AppName.get());
		
		// Set system tray icon
		if (Util.IS_LINUX)
			// On Linux, the 16x16 pixel icon would be too small
			trayItem.setImage(Img.DOCFETCHER_24.get());
		else
			trayItem.setImage(Img.DOCFETCHER_16.get());
		
		final Menu trayMenu = new Menu(shell, SWT.POP_UP);
		MenuItem restoreItem = new MenuItem(trayMenu, SWT.PUSH);
		MenuItem closeItem = new MenuItem(trayMenu, SWT.PUSH);
		restoreItem.setText(Msg.restore_app.get());
		closeItem.setText(Msg.exit.get());
		trayMenu.setDefaultItem(restoreItem);
		
		/*
		 * Event handling
		 */
		// Open system tray menu when the user clicks on it
		trayItem.addMenuDetectListener(new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent e) {
				trayMenu.setVisible(true);
			}
		});
		
		// Shut down application when user clicks on the 'close' tray item
		closeItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtShutdown.fire(null);
			}
		});
		
		// Restore application when user clicks on the 'restore' item or doubleclicks on the tray icon.
		Listener appRestorer = new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				restore();
			}
		};
		trayItem.addListener(SWT.Selection, appRestorer);
		restoreItem.addListener(SWT.Selection, appRestorer);
		
		evtHiding.fire(null);
		
		/*
		 * For some reason the shell will have the wrong location without this
		 * when brought back from system tray.
		 */
		shellLocation = shell.getLocation();
		shell.setVisible(false);
	}
	
	/**
	 * Returns whether the shell is hidden in the system tray.
	 */
	public boolean isHidden() {
		return trayItem != null;
	}
	
	/**
	 * Restores the shell from the system tray.
	 */
	public void restore() {
		if (trayItem == null)
			return;
		trayItem.dispose();
		trayItem = null;
		
		shell.setVisible(true);
		shell.forceActive();
		shell.setMinimized(false);
		shell.setLocation(shellLocation);
		shellLocation = null;
		
		evtRestored.fire(null);
	}
	
}
