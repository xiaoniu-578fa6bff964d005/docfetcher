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
	private final Text counter;
	private final ToolItem upBt;
	private final ToolItem downBt;
	private final ToolItem highlightBt;
	@NotNull private HighlightingText textViewer;
	
	@Nullable private Integer currentOcc;
	
	public HighlightingToolBarWithTextViewer(@NotNull Composite toolBarParent) {
		toolBarComp = new CustomBorderComposite(toolBarParent);
		int margin = Util.IS_WINDOWS ? 2 : 0;
		toolBarComp.setLayout(Util.createGridLayout(2, false, margin, 0));
		
		int textStyle = SWT.BORDER | SWT.SINGLE | SWT.CENTER | SWT.READ_ONLY;
		counter = new Text(toolBarComp, textStyle);
		GridData counterGridData = new GridData(SWT.RIGHT, SWT.FILL, true, true);
		counterGridData.minimumWidth = Util.BTW;
		counter.setLayoutData(counterGridData);
		counter.setBackground(Col.WIDGET_BACKGROUND.get());
		counter.setToolTipText(Msg.occurrence_count.get());
		
		ToolBar toolBar = new ToolBar(toolBarComp, SWT.FLAT);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		class ButtonHandler extends SelectionAdapter {
			private final boolean isDownBt;
			
			private ButtonHandler(boolean isDownBt) {
				this.isDownBt = isDownBt;
			}
			public void widgetSelected(SelectionEvent e) {
				moveSelection(this.isDownBt);
			}
		}
		
		ToolItemFactory tif = new ToolItemFactory(toolBar);
		tif.enabled(false);
		
		upBt = tif.image(Img.ARROW_UP.get()).toolTip(Msg.prev_occurrence.get())
				.listener(new ButtonHandler(false)).create();
		
		downBt = tif.image(Img.ARROW_DOWN.get()).toolTip(Msg.next_occurrence.get())
				.listener(new ButtonHandler(true)).create();
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		tif.style(SWT.CHECK);
		
		highlightBt = tif.image(Img.HIGHLIGHT.get())
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
		createToolItems(tif);
	}
	
    private void moveSelection(boolean isDownBt) {
        Integer newOcc = textViewer.goTo(isDownBt);
        if (newOcc != null) {
            currentOcc = newOcc;
            updateCounter();
        }
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
					updateCounter();
				}
			}
		});
		
		return textViewer.getControl();
	}
	
	private void updateCounter() {
		if (currentOcc != null)
			counter.setText(currentOcc + "/" + textViewer.getOccCount()); //$NON-NLS-1$
		else
			counter.setText(String.valueOf(textViewer.getOccCount()));
	}
	
	public void setUseMonoFont(boolean useMonoFont) {
		textViewer.setUseMonoFont(useMonoFont);
	}
	
	public final void setText(@NotNull HighlightedString string) {
		textViewer.setText(string);
		int occCount = string.getRangeCount();
		counter.setText(String.valueOf(occCount));
		upBt.setEnabled(occCount > 0);
		downBt.setEnabled(occCount > 0);
		currentOcc = null;
		highlightBt.setEnabled(occCount > 0);
        if (SettingsConf.Bool.AutoScrollToFirstMatch.get())
            moveSelection(true);
	}
	
	public final void appendText(@NotNull HighlightedString string) {
		textViewer.appendText(string);
		updateCounter();
		if (string.getRangeCount() > 0) {
			upBt.setEnabled(true);
			downBt.setEnabled(true);
			highlightBt.setEnabled(true);
		}
	}
	
	public final void clear() {
		textViewer.clear();
		counter.setText("");
		upBt.setEnabled(false);
		downBt.setEnabled(false);
		currentOcc = null;
		highlightBt.setEnabled(false);
	}

}
