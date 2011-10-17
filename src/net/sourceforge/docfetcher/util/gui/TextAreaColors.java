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

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Widget;

/**
 * @author Tran Nam Quang
 */
abstract class TextAreaColors {
	
	@Nullable private Color lastBackground;
	@Nullable private Color foreground;
	
	public TextAreaColors(@NotNull Widget widget) {
		Util.checkNotNull(widget);
		
		widget.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (foreground != null)
					foreground.dispose();
			}
		});
	}
	
	@NotNull
	public final Color getForeground() {
		Color background = getBackground();
		if (lastBackground == null) {
			lastBackground = background;
			assert foreground == null;
			foreground = createForeground(background);
		}
		else if (!lastBackground.equals(background)) {
			lastBackground = background;
			foreground.dispose();
			foreground = createForeground(background);
		}
		return foreground;
	}
	
	// Caller is responsible for disposing the background color
	@NotNull
	protected abstract Color getBackground();
	
	/**
	 * Returns a suitable text foreground color for the given background color.
	 * The returned color is either black or white, depending on the perceived
	 * luminance of the given background color.
	 */
	@NotNull
	private static Color createForeground(@NotNull Color background) {
		int r = background.getRed();
		int g = background.getGreen();
		int b = background.getBlue();
		double a = 1 - (0.299 * r + 0.587 * g + 0.114 * b) / 255;
		int d = a < 0.5 ? 0 : 255;
		return new Color(background.getDevice(), d, d, d);
	}

}
