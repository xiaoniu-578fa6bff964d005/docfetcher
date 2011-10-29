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

package net.sourceforge.docfetcher.gui.pref;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Tran Nam Quang
 */
final class StyledLabel extends Canvas {
	
	private static final int MARGIN = 2;
	
	@NotNull private String text;
	@Nullable private Font font;
	
	public StyledLabel(@NotNull Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		setBackground(Col.LIST_BACKGROUND.get());
		setForeground(Col.LIST_FOREGROUND.get());
		
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (font != null)
					e.gc.setFont(font);
				Point ext = e.gc.stringExtent(text);
				int x = (e.width - ext.x) / 2;
				int y = (e.height - ext.y) / 2;
				e.gc.drawString(text, x, y);
			}
		});
	}
	
	public void setFont(@Nullable Font font) {
		this.font = font;
		redraw();
	}
	
	public void setText(@NotNull String text) {
		Util.checkNotNull(text);
		this.text = text;
		redraw();
	}
	
	public Point computeSize(int wHint, int hHint, boolean changed) {
		GC gc = new GC(getDisplay());
		if (font != null)
			gc.setFont(font);
		Point ext = gc.stringExtent(text);
		gc.dispose();
		int width = wHint == SWT.DEFAULT ? ext.x + MARGIN : wHint;
		int height = hHint == SWT.DEFAULT ? ext.y + MARGIN : hHint;
		return new Point(width, height);
	}

}
