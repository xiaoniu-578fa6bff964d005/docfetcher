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

package net.sourceforge.docfetcher.util.gui;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

/**
 * @author Tran Nam Quang
 */
public final class SystemFont {
	
	private final List<Control> controls = new ArrayList<Control>();
	@NotNull private Font font;
	
	public SystemFont(	@NotNull final Widget widget,
						final int height,
						final int style) {
		Util.checkNotNull(widget);
		font = getSystemFont(widget, height, style);
		
		widget.getDisplay().addListener(SWT.Settings, new Listener() {
			public void handleEvent(Event event) {
				Font newFont = getSystemFont(widget, height, style);
				for (Control control : controls)
					control.setFont(newFont);
				font.dispose();
				font = newFont;
			}
		});
		
		widget.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				font.dispose();
			}
		});
	}
	
	/**
	 * Returns a new font derived from the SWT system font by setting the given
	 * font and style. The height argument will be ignored if its value is less
	 * than or equal to zero. The caller is responsible for disposing the
	 * returned font.
	 */
	@NotNull
	private static Font getSystemFont(	@NotNull Widget widget,
	                                  	int height,
										int style) {
		Display display = widget.getDisplay();
		FontData fontData = display.getSystemFont().getFontData()[0];
		if (height > 0)
			fontData.setHeight(height);
		fontData.setStyle(style);
		return new Font(display, fontData);
	}
	
	public void applyTo(@NotNull final Control control) {
		Util.checkNotNull(control);
		if (controls.contains(control))
			return;
		controls.add(control);
		control.setFont(font);
		
		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				controls.remove(control);
			}
		});
	}

}
