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

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.gui.Col;
import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.gui.CustomBorderComposite;
import net.sourceforge.docfetcher.model.search.HighlightedString;

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
	
	@NotNull private Text counter;
	@NotNull private ToolItem upBt;
	@NotNull private ToolItem downBt;
	@NotNull private HighlightingText textViewer;
	
	@Nullable private Integer currentOcc;

	public TextPreview(@NotNull Composite parent) {
		super(parent);
	}
	
	@NotNull
	protected Control createToolBar(@NotNull Composite parent) {
		CustomBorderComposite comp = new CustomBorderComposite(parent);
		comp.setLayout(Util.createGridLayout(2, false, 0, 0));
		
		// TODO i18n x3
		
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
		
		upBt = Util.createToolItem(
			toolBar, Img.ARROW_UP.get(), null, "prev_occurrence",
			new ButtonHandler(false));
		upBt.setEnabled(false);
		
		downBt = Util.createToolItem(
			toolBar, Img.ARROW_DOWN.get(), null, "next_occurrence",
			new ButtonHandler(true));
		downBt.setEnabled(false);
		
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
	}
	
	public void setText(@NotNull HighlightedString string) {
		textViewer.setText(string);
		int occCount = textViewer.getOccCount();
		counter.setText(occCount + "");
		upBt.setEnabled(occCount > 0);
		downBt.setEnabled(occCount > 0);
		currentOcc = null;
	}

	public void appendText(@NotNull HighlightedString string) {
		textViewer.appendText(string);
		updateCounter();
		if (string.getRangeCount() > 0) {
			upBt.setEnabled(true);
			downBt.setEnabled(true);
		}
	}

}
