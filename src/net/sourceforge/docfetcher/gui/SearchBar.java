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
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.collect.MemoryList;
import net.sourceforge.docfetcher.util.gui.ToolItemFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

/**
 * @author Tran Nam Quang
 */
public final class SearchBar {
	
	public final Event<String> evtSearch = new Event<String>();
	public final Event<Void> evtHideInSystemTray = new Event<Void>();
	public final Event<Void> evtOpenManual = new Event<Void>();
	
	private final Composite comp;
	private final Combo searchBox;
	private final Button searchBt;
	private final MemoryList<String> searchHistory;
	
	public SearchBar(@NotNull Composite parent) {
		comp = new CustomBorderComposite(parent);
		searchBox = new Combo(comp, SWT.BORDER);
		searchBox.setVisibleItemCount(ProgramConf.Int.SearchHistorySize.get());
		Util.selectAllOnFocus(searchBox);
		
		searchBox.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				String query = searchBox.getText().trim();
				if (!query.isEmpty() && Util.isEnterKey(e.keyCode))
					evtSearch.fire(query);
			}
		});
		
		// Load search history
		searchHistory = new MemoryList<String>(ProgramConf.Int.SearchHistorySize.get());
		searchHistory.addAll(SettingsConf.StrList.SearchHistory.get()); // may discard items
		searchBox.setItems(getHistoryArray());
		
		searchBt = new Button(comp, SWT.PUSH);
		searchBt.setText("Search");
		searchBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String query = searchBox.getText().trim();
				if (!query.isEmpty())
					evtSearch.fire(query);
			}
		});
		
		// Create toolbar
		final ToolBar toolBar = new ToolBar(comp, SWT.FLAT);
		ToolItemFactory tif = new ToolItemFactory(toolBar);
		
		// TODO i18n
		
		tif.image(Img.HELP.get()).toolTip("Open Manual")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						evtOpenManual.fire(null);
					}
				}).create();
		
		tif.image(Img.PREFERENCES.get()).toolTip("Preferences")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						PrefDialog prefDialog = new PrefDialog(comp.getShell());
						prefDialog.open();
					}
				}).create();

		tif.image(Img.BROWSER.get()).toolTip("Web Interface")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						WebInterfaceDialog dialog = new WebInterfaceDialog(comp.getShell());
						dialog.open();
					}
				}).create();
		
		tif.image(Img.HIDE.get()).toolTip("to_systray")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						evtHideInSystemTray.fire(null);
					}
				}).create();
		
		comp.setLayout(Util.createGridLayout(3, false, 0, 0));
		
		final int searchBoxMaxWidth = ProgramConf.Int.SearchBoxMaxWidth.get();
		final GridData searchBoxGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		searchBoxGridData.widthHint = searchBoxMaxWidth;
		searchBox.setLayoutData(searchBoxGridData);
		
		searchBt.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		
		// Make the search box smaller when there's not enough space left
		comp.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int spaceLeft = comp.getSize().x;
				spaceLeft -= toolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				spaceLeft -= searchBt.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				
				/*
				 * The minimum value for this is platform dependent. 10 pixel
				 * should be large enough for all platforms.
				 */
				spaceLeft -= 10;
				
				searchBoxGridData.widthHint = Util.clamp(spaceLeft, 0, searchBoxMaxWidth);
				comp.layout();
			}
		});
	}
	
	public void addToSearchHistory(@NotNull String query) {
		Util.checkNotNull(query);
		searchHistory.add(query);
		String[] historyArray = getHistoryArray();
		SettingsConf.StrList.SearchHistory.set(historyArray);
		searchBox.setItems(historyArray);
		searchBox.setText(query);
	}
	
	@NotNull
	private String[] getHistoryArray() {
		return searchHistory.toArray(new String[searchHistory.size()]);
	}
	
	@NotNull
	public Control getControl() {
		return comp;
	}

	public boolean isEnabled() {
		return searchBox.isEnabled();
	}

	public void setEnabled(boolean enabled) {
		searchBox.setEnabled(enabled);
		searchBt.setEnabled(enabled);
	}

	public boolean setFocus() {
		return searchBox.setFocus();
	}

}
