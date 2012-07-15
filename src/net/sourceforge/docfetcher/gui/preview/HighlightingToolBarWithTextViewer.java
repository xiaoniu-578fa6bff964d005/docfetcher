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
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.ToolItemFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Tran Nam Quang
 */
class HighlightingToolBarWithTextViewer {
	
	private final Composite toolBarComp;
	@NotNull private HighlightingText textViewer;
	private final ToolItem highlightBt;
	
	private final Text pageNumField;
	private final ToolItem prevBt;
	private final ToolItem nextBt;
	
	private final Text occField;
	private final ToolItem upBt;
	private final ToolItem downBt;
	
	@Nullable private Integer currentOcc; // one-based
	private int occCount = 0;
	@Nullable private Integer pageIndex; // zero-based
	private final List<HighlightedString> pages = new ArrayList<HighlightedString>();
	
	public HighlightingToolBarWithTextViewer(@NotNull Composite toolBarParent) {
		toolBarComp = new CustomBorderComposite(toolBarParent);
		int margin = Util.IS_WINDOWS ? 2 : 0;
		toolBarComp.setLayout(Util.createGridLayout(4, false, margin, 0));
		
		int textStyle = SWT.BORDER | SWT.SINGLE | SWT.CENTER | SWT.READ_ONLY;
		pageNumField = new Text(toolBarComp, textStyle);
		GridData pageNumGridData = new GridData(SWT.RIGHT, SWT.FILL, true, true);
		pageNumGridData.minimumWidth = Util.BTW;
		pageNumField.setLayoutData(pageNumGridData);
		pageNumField.setBackground(Col.WIDGET_BACKGROUND.get());
		pageNumField.setToolTipText(Msg.page_num.get());
		
		ToolBar toolBar1 = new ToolBar(toolBarComp, SWT.FLAT);
		toolBar1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		new ToolItem(toolBar1, SWT.SEPARATOR);
		
		ToolItemFactory tif1 = new ToolItemFactory(toolBar1);
		tif1.enabled(false);
		
		prevBt = tif1.image(Img.ARROW_LEFT.get())
				.toolTip(Msg.prev_page.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						pageIndex = Math.max(0, pageIndex - 1);
						currentOcc = null;
						updatePage();
						updateOccField();
					}
				}).create();

		nextBt = tif1.image(Img.ARROW_RIGHT.get())
				.toolTip(Msg.next_page.get())
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						pageIndex = Math.min(pages.size() - 1, pageIndex + 1);
						currentOcc = null;
						updatePage();
						updateOccField();
					}
				}).create();
		
		new ToolItem(toolBar1, SWT.SEPARATOR);
		
		occField = new Text(toolBarComp, textStyle);
		GridData counterGridData = new GridData(SWT.FILL, SWT.FILL, false, true);
		counterGridData.minimumWidth = Util.BTW;
		occField.setLayoutData(counterGridData);
		occField.setBackground(Col.WIDGET_BACKGROUND.get());
		occField.setToolTipText(Msg.occurrence_count.get());
		
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
	}
	
    private void moveSelection(boolean isDownBt) {
        Integer newOcc = textViewer.goTo(isDownBt);
        
        if (newOcc != null) { // Occurrence found on current page
            currentOcc = relativeToAbsoluteOccurrence(newOcc);
            updateOccField();
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
        			updateOccField();
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
        			updateOccField();
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
		return toolBarComp;
	}
	
	@NotNull
	public final Control createTextViewer(@NotNull Composite parent) {
		textViewer = new HighlightingText(parent);
		
		textViewer.getControl().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Point sel = textViewer.getControl().getSelection();
				if (sel.x == sel.y) {
					currentOcc = null;
					updateOccField();
				}
			}
		});
		
		return textViewer.getControl();
	}
	
	private void updateOccField() {
		if (currentOcc != null)
			occField.setText(currentOcc + "/" + occCount); //$NON-NLS-1$
		else
			occField.setText(String.valueOf(occCount));
	}
	
	private void updatePageToolbar() {
		if (pageIndex == null)
			pageNumField.setText("");
		else
			pageNumField.setText((pageIndex + 1) + "/" + pages.size());
		prevBt.setEnabled(pageIndex != null && pageIndex > 0);
		nextBt.setEnabled(pageIndex != null && pageIndex < pages.size() - 1);
	}
	
	private void updatePage() {
		HighlightedString string = pages.get(pageIndex);
		textViewer.setText(string);
		updatePageToolbar();
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
		
		updatePageToolbar();
		occField.setText(String.valueOf(occCount));
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
		
		updatePageToolbar();
		updateOccField();
		if (string.getRangeCount() > 0) {
			upBt.setEnabled(true);
			downBt.setEnabled(true);
			highlightBt.setEnabled(true);
		}
	}
	
	public final void clear() {
		currentOcc = null;
		occCount = 0;
		pageIndex = null;
		pages.clear();
		
		textViewer.clear();
		updatePageToolbar();
		occField.setText("");
		upBt.setEnabled(false);
		downBt.setEnabled(false);
		highlightBt.setEnabled(false);
	}

}
