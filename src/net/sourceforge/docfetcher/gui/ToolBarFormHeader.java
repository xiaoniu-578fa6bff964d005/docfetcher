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

package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

class ToolBarFormHeader extends Composite {
	
	@NotNull private final Label imageLabel;
	@NotNull private final Label textLabel;
	@Nullable private final Composite toolBar;
	
	@NotNull private Color bgCol;
	@NotNull private Color borderCol;

	public ToolBarFormHeader(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(Util.createGridLayout(3, false, 5, 5));
		
		imageLabel = new Label(this, SWT.NONE);
		imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		textLabel = new Label(this, SWT.NONE);
		textLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		// margin=1 for the fill layout to leave room for the custom border
		toolBar = new Composite(this, SWT.NONE);
		toolBar.setLayout(Util.createFillLayout(1));
		createToolBar(toolBar);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		updateColorScheme();
		updateLayout();
		
		// Update colors on theme changes
		getDisplay().addListener(SWT.Settings, new Listener() {
			public void handleEvent(Event event) {
				updateColorScheme();
			}
		});
		
		// Draw borders
		PaintListener borderPainter = new PaintListener() {
			public void paintControl(PaintEvent e) {
				Point size = ((Control)e.widget).getSize();
				e.gc.setForeground(borderCol);
				e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);
			}
		};
		addPaintListener(borderPainter);
		toolBar.addPaintListener(borderPainter);
		
		// Dispose colors
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				bgCol.dispose();
				borderCol.dispose();
			}
		});
	}
	
	/**
	 * Creates a background and border color, and sets them on the various
	 * widgets. The background color is based on the title background color, but
	 * with adjusted saturation and brightness. The border color is derived from
	 * the custom background color by reducing the brightness to 75%.
	 */
	private void updateColorScheme() {
		float[] titleHsb = Col.TITLE_BACKGROUND.get().getRGB().getHSB();
		float hue = titleHsb[0];
		float sat = Math.min(0.2f, titleHsb[1]);
		float br = Col.WIDGET_BACKGROUND.get().getRGB().getHSB()[2];
		
		RGB newBgRGB = new RGB(hue, sat, br);
		RGB newBorderRGB = new RGB(hue, sat, br * 0.75f);
		
		// Compare new colors with cached colors
		if (bgCol != null) {
			assert borderCol != null;
			RGB oldBgRGB = bgCol.getRGB();
			if (oldBgRGB.equals(newBgRGB)) {
				return;
			}
			else {
				bgCol.dispose();
				borderCol.dispose();
			}
		}
		
		bgCol = new Color(getDisplay(), newBgRGB);
		borderCol = new Color(getDisplay(), newBorderRGB);
		
		// Set background color
		imageLabel.setBackground(bgCol);
		setBackground(bgCol);
		textLabel.setBackground(bgCol);
	}
	
	private void updateLayout() {
		int colCount = 1;
		GridData imageData = (GridData) imageLabel.getLayoutData();
		if (imageLabel.getImage() == null) {
			imageData.exclude = true;
		} else {
			imageData.exclude = false;
			colCount++;
		}
		if (toolBar != null)
			colCount++;
		((GridLayout) getLayout()).numColumns = colCount;
		layout();
	}
	
	@Nullable
	public final Image getImage() {
		return imageLabel.getImage();
	}

	public final void setImage(@Nullable Image image) {
		imageLabel.setImage(image);
		updateLayout();
	}

	@NotNull
	public final String getText() {
		return textLabel.getText();
	}

	public final void setText(@NotNull String text) {
		textLabel.setText(text);
	}

	@Nullable
	protected Control createToolBar(Composite parent) {
		return null;
	}

}
