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

package net.sourceforge.docfetcher.gui.preview;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.CustomBorderComposite;
import net.sourceforge.docfetcher.model.search.HighlightedString;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.ToolItemFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Tran Nam Quang
 */
class HighlightingToolBarWithTextViewer {
	
	private final Composite barComp;
	@NotNull private HighlightingText textViewer;
	private final ToolItem highlightBt;
	
	private final RangeField pageNumField;
	private final ToolBar pageToolbar;
	private final ToolItem prevBt;
	private final ToolItem nextBt;
	
	private final RangeField occField;
	private final ToolItem upBt;
	private final ToolItem downBt;
	
	@Nullable private Integer currentOcc; // one-based
	private int occCount = 0;
	@Nullable private Integer pageIndex; // zero-based
	private final List<HighlightedString> pages = new ArrayList<HighlightedString>();
	
	public HighlightingToolBarWithTextViewer(@NotNull Composite toolBarParent) {
		barComp = new CustomBorderComposite(toolBarParent);
		int margin = Util.IS_WINDOWS ? 2 : 0;
		barComp.setLayout(Util.createGridLayout(2, false, margin, 0));
		
		Label indentArea = new Label(barComp, SWT.NONE);
		indentArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite toolBarComp = new Composite(barComp, SWT.NONE);
		toolBarComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		toolBarComp.setLayout(Util.createGridLayout(4, false, margin, 0));
		
		pageNumField = new RangeField(toolBarComp);
		GridData pageNumGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		pageNumGridData.minimumWidth = Util.BTW;
		pageNumField.getControl().setLayoutData(pageNumGridData);
		pageNumField.getControl().setToolTipText(Msg.page_num.get());
		
		pageToolbar = new ToolBar(toolBarComp, SWT.FLAT);
		pageToolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		new ToolItem(pageToolbar, SWT.SEPARATOR);
		
		ToolItemFactory tif1 = new ToolItemFactory(pageToolbar);
		tif1.enabled(false);
		
		prevBt = tif1.image(Img.ARROW_LEFT.get())
				.toolTip(Msg.prev_page.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						goToPage(pageIndex - 1);
					}
				}).create();

		nextBt = tif1.image(Img.ARROW_RIGHT.get())
				.toolTip(Msg.next_page.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						goToPage(pageIndex + 1);
					}
				}).create();
		
		new ToolItem(pageToolbar, SWT.SEPARATOR);
		
		occField = new RangeField(toolBarComp);
		GridData occGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		occGridData.minimumWidth = Util.BTW;
		occField.getControl().setLayoutData(occGridData);
		occField.getControl().setToolTipText(Msg.occurrence_count.get());
		
		ToolBar toolBar2 = new ToolBar(toolBarComp, SWT.FLAT);
		toolBar2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		new ToolItem(toolBar2, SWT.SEPARATOR);
		
		ToolItemFactory tif2 = new ToolItemFactory(toolBar2);
		tif2.enabled(false);
		
		upBt = tif2.image(Img.ARROW_UP.get()).toolTip(Msg.prev_occurrence.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						moveSelection(false);
					}
				}).create();
		
		downBt = tif2.image(Img.ARROW_DOWN.get()).toolTip(Msg.next_occurrence.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						moveSelection(true);
					}
				}).create();
		
		new ToolItem(toolBar2, SWT.SEPARATOR);
		tif2.style(SWT.CHECK);
		
		highlightBt = tif2.image(Img.HIGHLIGHT.get())
				.toolTip(Msg.highlighting_on_off.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						boolean selected = highlightBt.getSelection();
						SettingsConf.Bool.HighlightingEnabled.set(selected);
						textViewer.updateHighlighting();
					}
				}).create();
		
		// Synchronize highlighting states across instances of this class
		SettingsConf.Bool.HighlightingEnabled.evtChanged.add(new Event.Listener<Boolean>() {
			public void update(Boolean eventData) {
				boolean selected = highlightBt.getSelection();
				if (!eventData.equals(selected)) {
					highlightBt.setSelection(eventData);
					textViewer.updateHighlighting();
				}
			}
		});
		
		highlightBt.setSelection(SettingsConf.Bool.HighlightingEnabled.get());
		createToolItems(tif2);
		
		pageNumField.evtValueChanged.add(new Event.Listener<Integer>() {
			public void update(Integer eventData) {
				// Switch from one-based to zero-based page number
				goToPage(eventData - 1);
			}
		});
		
		occField.evtValueChanged.add(new Event.Listener<Integer>() {
			public void update(Integer eventData) {
				if (occCount == 0) {
					occField.setRange(0, 0);
					return;
				}
				int targetOcc = Util.clamp(eventData, 1, occCount);
				int sum = 0;
				for (int i = 0; i < pages.size(); i++) {
					int start = sum + 1;
					int occCountOnPage = pages.get(i).getRangeCount();
					int end = start + occCountOnPage;
					if (start <= targetOcc && targetOcc < end) {
						currentOcc = targetOcc;
						occField.setRange(currentOcc, occCount);
						pageIndex = i;
						updatePage();
						textViewer.goTo(targetOcc - sum);
						break;
					}
					sum += occCountOnPage;
				}
			}
		});
	}
	
    private void moveSelection(boolean isDownBt) {
        Integer newOcc = textViewer.goTo(isDownBt);
        
        if (newOcc != null) { // Occurrence found on current page
            currentOcc = relativeToAbsoluteOccurrence(newOcc);
            occField.setRange(currentOcc, occCount);
        }
        else { // Go to nearest page containing an occurrence, if one exists
        	if (isDownBt) {
        		for (int i = pageIndex + 1; i < pages.size(); i++) {
        			HighlightedString string = pages.get(i);
        			if (string.getRangeCount() == 0)
        				continue;
        			pageIndex = i;
        			updatePage();
        			Integer occ = textViewer.goTo(true);
        			currentOcc = relativeToAbsoluteOccurrence(occ);
        			occField.setRange(currentOcc, occCount);
        			break;
        		}
        	} else {
        		for (int i = pageIndex - 1; i >= 0; i--) {
        			HighlightedString string = pages.get(i);
        			if (string.getRangeCount() == 0)
        				continue;
        			pageIndex = i;
        			updatePage();
        			Integer occ = textViewer.goToLast();
        			currentOcc = relativeToAbsoluteOccurrence(occ);
        			occField.setRange(currentOcc, occCount);
        			break;
        		}
        	}
        }
    }
    
    // Converts relative to absolute occurrence number
    private int relativeToAbsoluteOccurrence(int occurrence) {
    	for (int i = 0; i < pageIndex; i++)
    		occurrence += pages.get(i).getRangeCount();
    	return occurrence;
    }
    
    protected void createToolItems(@NotNull ToolItemFactory tif) {}
	
	@NotNull
	public final Composite getToolBar() {
		return barComp;
	}
	
	@NotNull
	public final Control createTextViewer(@NotNull Composite parent) {
		textViewer = new HighlightingText(parent);
		
		textViewer.getControl().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Point sel = textViewer.getControl().getSelection();
				if (sel.x == sel.y) {
					currentOcc = null;
					occField.setRange(currentOcc, occCount);
				}
			}
		});
		
		return textViewer.getControl();
	}
	
	private void updatePageToolbar(boolean visible) {
		pageNumField.getControl().setVisible(visible);
		pageToolbar.setVisible(visible);
		if (visible == false)
			return;
		if (pageIndex == null)
			pageNumField.clear();
		else
			pageNumField.setRange(pageIndex + 1, pages.size());
		prevBt.setEnabled(pageIndex != null && pageIndex > 0);
		nextBt.setEnabled(pageIndex != null && pageIndex < pages.size() - 1);
	}
	
	private void updatePage() {
		HighlightedString string = pages.get(pageIndex);
		textViewer.setText(string);
		updatePageToolbar(true);
	}
	
	private void goToPage(int pageIndex) {
		pageIndex = Util.clamp(pageIndex, 0, pages.size() - 1);
		if (this.pageIndex == pageIndex)
			return;
		this.pageIndex = pageIndex;
		updatePage();
		currentOcc = null;
		occField.setRange(currentOcc, occCount);
	}
	
	public void setUseMonoFont(boolean useMonoFont) {
		textViewer.setUseMonoFont(useMonoFont);
	}
	
	public final void setText(@NotNull HighlightedString string) {
		currentOcc = null;
		occCount = string.getRangeCount();
		pageIndex = 0;
		pages.clear();
		pages.add(string);
		
		textViewer.getControl().setRedraw(false);
		textViewer.setText(string);
		
		updatePageToolbar(false);
		occField.setRange(currentOcc, occCount);
		upBt.setEnabled(occCount > 0);
		downBt.setEnabled(occCount > 0);
		
		highlightBt.setEnabled(occCount > 0);
        if (SettingsConf.Bool.AutoScrollToFirstMatch.get())
            moveSelection(true);
        textViewer.getControl().setRedraw(true);
	}
	
	public final void appendPage(@NotNull HighlightedString string) {
		occCount += string.getRangeCount();
		if (pages.isEmpty()) {
			pageIndex = 0;
			textViewer.appendText(string);
		}
		pages.add(string);
		
		updatePageToolbar(true);
		occField.setRange(currentOcc, occCount);
		if (string.getRangeCount() > 0) {
			upBt.setEnabled(true);
			downBt.setEnabled(true);
			highlightBt.setEnabled(true);
		}
	}
	
	public final void clear(boolean showPageToolbar) {
		currentOcc = null;
		occCount = 0;
		pageIndex = null;
		pages.clear();
		
		textViewer.clear();
		updatePageToolbar(showPageToolbar);
		occField.clear();
		upBt.setEnabled(false);
		downBt.setEnabled(false);
		highlightBt.setEnabled(false);
	}

}
