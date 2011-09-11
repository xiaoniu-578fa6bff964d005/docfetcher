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
final class TextPreview extends ToolBarForm {
	
	public final Event<Void> evtTextToHtmlBt = new Event<Void>();
	
	@NotNull private Text counter;
	@NotNull private ToolItem upBt;
	@NotNull private ToolItem downBt;
	@NotNull private ToolItem highlightBt;
	@NotNull private ToolItem htmlBt;
	@NotNull private HighlightingText textViewer;
	
	@Nullable private Integer currentOcc;

	public TextPreview(@NotNull Composite parent) {
		super(parent);
	}
	
	@NotNull
	protected Control createToolBar(@NotNull Composite parent) {
		CustomBorderComposite comp = new CustomBorderComposite(parent);
		comp.setLayout(Util.createGridLayout(2, false, 0, 0));
		
		// TODO i18n for counter and buttons
		
		int textStyle = SWT.BORDER | SWT.SINGLE | SWT.CENTER | SWT.READ_ONLY;
		counter = new Text(comp, textStyle);
		GridData counterGridData = new GridData(SWT.RIGHT, SWT.FILL, true, true);
		counterGridData.minimumWidth = 75;
		counter.setLayoutData(counterGridData);
		counter.setBackground(Col.WIDGET_BACKGROUND.get());
		counter.setToolTipText("occurrence_count");
		
		ToolBar toolBar = new ToolBar(comp, SWT.FLAT);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		class ButtonHandler extends SelectionAdapter {
			private final boolean isDownBt;
			
			private ButtonHandler(boolean isDownBt) {
				this.isDownBt = isDownBt;
			}
			public void widgetSelected(SelectionEvent e) {
				Integer newOcc = textViewer.goTo(isDownBt);
				if (newOcc != null) {
					currentOcc = newOcc;
					updateCounter();
				}
			}
		}
		
		ToolItemFactory tif = new ToolItemFactory(toolBar);
		tif.enabled(false);
		
		upBt = tif.image(Img.ARROW_UP.get()).toolTip("prev_occurrence")
				.listener(new ButtonHandler(false)).create();
		
		downBt = tif.image(Img.ARROW_DOWN.get()).toolTip("next_occurrence")
				.listener(new ButtonHandler(true)).create();
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		tif.style(SWT.CHECK);
		
		highlightBt = tif.image(Img.HIGHLIGHT.get())
				.toolTip("Highlighting On/Off")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						boolean selected = highlightBt.getSelection();
						SettingsConf.Bool.HighlightingEnabled.set(selected);
						textViewer.updateHighlighting();
					}
				}).create();
		
		highlightBt.setSelection(SettingsConf.Bool.HighlightingEnabled.get());
		tif.style(SWT.PUSH);
		
		htmlBt = tif.image(Img.BUILDING_BLOCKS.get())
				.toolTip("use_embedded_html_viewer")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						evtTextToHtmlBt.fire(null);
					}
				}).create();
		
		return comp;
	}
	
	private void updateCounter() {
		if (currentOcc != null)
			counter.setText(currentOcc + "/" + textViewer.getOccCount());
		else
			counter.setText(textViewer.getOccCount() + "");
	}
	
	@NotNull
	protected Control createContents(@NotNull Composite parent) {
		// TODO put text viewer into a container with margins
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
	
	public void clear() {
		textViewer.clear();
		counter.setText("");
		upBt.setEnabled(false);
		downBt.setEnabled(false);
		currentOcc = null;
		htmlBt.setEnabled(false);
		highlightBt.setEnabled(false);
	}
	
	public void setHtmlButtonEnabled(boolean enabled) {
		htmlBt.setEnabled(enabled);
	}
	
	public void setText(@NotNull HighlightedString string) {
		textViewer.setText(string);
		int occCount = textViewer.getOccCount();
		counter.setText(occCount + "");
		upBt.setEnabled(occCount > 0);
		downBt.setEnabled(occCount > 0);
		currentOcc = null;
		highlightBt.setEnabled(occCount > 0);
	}

	public void appendText(@NotNull HighlightedString string) {
		textViewer.appendText(string);
		updateCounter();
		if (string.getRangeCount() > 0) {
			upBt.setEnabled(true);
			downBt.setEnabled(true);
			highlightBt.setEnabled(true);
		}
	}

}
