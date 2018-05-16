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

import java.io.File;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.pref.PrefDialog;
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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

/**
 * @author Tran Nam Quang
 */
public final class SearchBar {

	private static final int MARGIN = Util.IS_WINDOWS ? 1 : 0;
	private static final int SPACING = 1;

	public final Event<String> evtSearch = new Event<String>();
	public final Event<Void> evtHideInSystemTray = new Event<Void>();
	public final Event<Void> evtOpenManual = new Event<Void>();
	public final Event<Void> evtOKClicked = new Event<Void> ();
	public final Event<Void> evtTraverse = new Event<Void> ();

	private final Composite comp;
	private final Combo searchBox;
	private final Button searchBt;
	private final ToolBar toolBar;
	private final MemoryList<String> searchHistory;

	public SearchBar(@NotNull Composite parent, @NotNull final File programConfFile) {
		comp = new CustomBorderComposite(parent) {
			public Point computeSize(int wHint, int hHint, boolean changed) {
				return SearchBar.this.computeSize(wHint, hHint);
			}
		};
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
		searchBox.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					e.doit = false;
					evtTraverse.fire(null);
				}
			}
		});

		// Load search history
		searchHistory = new MemoryList<String>(ProgramConf.Int.SearchHistorySize.get());
		searchHistory.addAll(SettingsConf.StrList.SearchHistory.get()); // may discard items
		searchBox.setItems(getHistoryArray());

		searchBt = new Button(comp, SWT.PUSH);
		searchBt.setText(Msg.search.get());
		searchBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String query = searchBox.getText().trim();
				if (!query.isEmpty())
					evtSearch.fire(query);
			}
		});

		toolBar = new ToolBar(comp, SWT.FLAT);
		ToolItemFactory tif = new ToolItemFactory(toolBar);

		tif.image(Img.HELP.get()).toolTip(Msg.open_manual.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						evtOpenManual.fire(null);
					}
				}).create();

		tif.image(Img.PREFERENCES.get()).toolTip(Msg.preferences.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						PrefDialog prefDialog = new PrefDialog(comp.getShell(), programConfFile);
						prefDialog.evtOKClicked.add(new Event.Listener<Void> () {
						    public void update(Void eventData) {
						    	evtOKClicked.fire(null);
						    }
						});
						prefDialog.open();
					}
				}).create();

		// TODO web interface
//		tif.image(Img.BROWSER.get()).toolTip(Msg.web_interface.get())
//				.listener(new SelectionAdapter() {
//					public void widgetSelected(SelectionEvent e) {
//						WebInterfaceDialog dialog = new WebInterfaceDialog(comp.getShell());
//						dialog.open();
//					}
//				}).create();

		/*
		 * On Ubuntu Unity, disable hiding in system tray. See bug #3457028 and
		 * follow-up bug #3457035.
		 */
		if (!Util.IS_UBUNTU_UNITY) {
			tif.image(Img.HIDE.get()).toolTip(Msg.to_systray.get())
			.listener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					evtHideInSystemTray.fire(null);
				}
			}).create();
		}

		// Make the search box smaller when there's not enough space left
		comp.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Point searchBoxSize = computeSize(searchBox);
				Point searchBtSize = computeSize(searchBt);
				Point toolBarSize = computeSize(toolBar);

				Rectangle clientArea = comp.getClientArea();
				int caWidth = clientArea.width;
				int caHeight = clientArea.height;

				int spaceLeft = caWidth;
				spaceLeft -= toolBarSize.x;
				spaceLeft -= searchBtSize.x;
				spaceLeft -= 5; // spacing between search button and toolbar

				int searchBoxMaxWidth = ProgramConf.Int.SearchBoxMaxWidth.get();
				int searchBoxWidth = Util.clamp(spaceLeft, 0, searchBoxMaxWidth);
				setBounds(searchBox, MARGIN, searchBoxWidth, caHeight, searchBoxSize.y);

				int searchBtX = MARGIN + searchBox.getSize().x + SPACING;
				setBounds(searchBt, searchBtX, searchBtSize.x, caHeight, searchBtSize.y);

				int toolBarX = caWidth - MARGIN - toolBarSize.x;
				setBounds(toolBar, toolBarX, toolBarSize.x, caHeight, toolBarSize.y);
			}
		});
	}

	private void setBounds(	@NotNull Control control,
							int x,
							int width,
							int compHeight,
							int controlHeight) {
		int y = Math.max(0, (compHeight - controlHeight) / 2);
		control.setLocation(x, y);
		control.setSize(width, Math.min(compHeight, controlHeight));
	}

	@NotNull
	private Point computeSize(int wHint, int hHint) {
		Point searchBoxSize = computeSize(searchBox);
		Point searchBtSize = computeSize(searchBt);
		Point toolBarSize = computeSize(toolBar);

		int width = 0;
		width += searchBoxSize.x;
		width += searchBtSize.x;
		width += toolBarSize.x;
		width += MARGIN * 2 + SPACING * 2;

		int height = Math.max(searchBoxSize.y, searchBtSize.y);
		height = Math.max(height, toolBarSize.y);
		height += MARGIN * 2;

		if (wHint != SWT.DEFAULT)
			width = wHint;
		if (hHint != SWT.DEFAULT)
			height = hHint;
		return new Point(width, height);
	}

	@NotNull
	private Point computeSize(@NotNull Control control) {
		return control.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
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
