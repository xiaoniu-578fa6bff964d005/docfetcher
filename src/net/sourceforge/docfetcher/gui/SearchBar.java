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

import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.MemoryList;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.gui.FormDataFactory;
import net.sourceforge.docfetcher.base.gui.ToolItemFactory;
import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormData;
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
	
	private final Composite comp;
	private final Combo searchBox;
	private final Button searchBt;
	private final MemoryList<String> searchHistory;
	
	public SearchBar(@NotNull Composite parent) {
		comp = new CustomBorderComposite(parent);
		searchBox = new Combo(comp, SWT.BORDER);
		searchBox.setVisibleItemCount(ProgramConf.Int.SearchHistorySize.get());
		Util.selectAllOnFocus(searchBox);
		
		// TODO bugfix: don't store empty queries in the search history
		// -> put this in the changelog when fixed
		
		searchBox.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				String query = searchBox.getText().trim();
				if (!query.isEmpty() && UtilGui.isEnterKey(e.keyCode))
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

		tif.image(Img.BROWSER.get()).toolTip("Web Interface")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						WebInterfaceDialog dialog = new WebInterfaceDialog(comp.getShell());
						dialog.open();
					}
				}).create();
		
		final int searchBoxMaxWidth = ProgramConf.Int.SearchBoxMaxWidth.get();
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).top().bottom().right().applyTo(toolBar);
		fdf.unright().left(searchBox).applyTo(searchBt);
		fdf.left().width(searchBoxMaxWidth).applyTo(searchBox);
		
		// Make the search box smaller when there's not enough space left
		comp.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int spaceLeft = comp.getSize().x;
				spaceLeft -= toolBar.getSize().x;
				spaceLeft -= searchBt.getSize().x;
				spaceLeft -= comp.getBorderWidth() * 2;
				FormData formData = (FormData) searchBox.getLayoutData();
				formData.width = Util.clamp(spaceLeft, 0, searchBoxMaxWidth);
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
