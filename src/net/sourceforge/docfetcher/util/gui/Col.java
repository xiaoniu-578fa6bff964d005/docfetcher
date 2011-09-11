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

package net.sourceforge.docfetcher.util.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * This enumeration provides convenient access to the SWT system colors.
 */
public enum Col {
	
	BLACK (SWT.COLOR_BLACK),
	BLUE (SWT.COLOR_BLUE),
	CYAN (SWT.COLOR_CYAN),
	DARK_BLUE (SWT.COLOR_DARK_BLUE),
	DARK_CYAN (SWT.COLOR_DARK_CYAN),
	DARK_GRAY (SWT.COLOR_DARK_GRAY),
	DARK_GREEN (SWT.COLOR_DARK_GREEN),
	DARK_MAGENTA (SWT.COLOR_DARK_MAGENTA),
	DARK_RED (SWT.COLOR_DARK_RED),
	DARK_YELLOW (SWT.COLOR_DARK_YELLOW),
	GRAY (SWT.COLOR_GRAY),
	GREEN (SWT.COLOR_GREEN),
	INFO_BACKGROUND (SWT.COLOR_INFO_BACKGROUND),
	INFO_FOREGROUND (SWT.COLOR_INFO_FOREGROUND),
	LIST_BACKGROUND (SWT.COLOR_LIST_BACKGROUND),
	LIST_FOREGROUND (SWT.COLOR_LIST_FOREGROUND),
	LIST_SELECTION (SWT.COLOR_LIST_SELECTION),
	LIST_SELECTION_TEXT (SWT.COLOR_LIST_SELECTION_TEXT),
	MAGENTA (SWT.COLOR_MAGENTA),
	RED (SWT.COLOR_RED),
	TITLE_BACKGROUND_GRADIENT (SWT.COLOR_TITLE_BACKGROUND_GRADIENT),
	TITLE_BACKGROUND (SWT.COLOR_TITLE_BACKGROUND),
	TITLE_FOREGROUND (SWT.COLOR_TITLE_FOREGROUND),
	TITLE_INACTIVE_BACKGROUND_GRADIENT (SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT),
	TITLE_INACTIVE_BACKGROUND (SWT.COLOR_TITLE_INACTIVE_BACKGROUND),
	TITLE_INACTIVE_FOREGROUND (SWT.COLOR_TITLE_INACTIVE_FOREGROUND),
	WHITE (SWT.COLOR_WHITE),
	WIDGET_BACKGROUND (SWT.COLOR_WIDGET_BACKGROUND),
	WIDGET_BORDER (SWT.COLOR_WIDGET_BORDER),
	WIDGET_DARK_SHADOW (SWT.COLOR_WIDGET_DARK_SHADOW),
	WIDGET_FOREGROUND (SWT.COLOR_WIDGET_FOREGROUND),
	WIDGET_HIGHLIGHT_SHADOW (SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW),
	WIDGET_LIGHT_SHADOW (SWT.COLOR_WIDGET_LIGHT_SHADOW),
	WIDGET_NORMAL_SHADOW (SWT.COLOR_WIDGET_NORMAL_SHADOW),
	YELLOW (SWT.COLOR_YELLOW),
	;
	
	private int styleBit;
	
	private Col(int styleBit) {
		this.styleBit = styleBit;
	}

	/**
	 * Returns the SWT color object for this color.
	 * <p>
	 * Note: This method should not be called before the application's display
	 * is created, otherwise it will attempt to create its own display, which
	 * may cause an SWTError.
	 */
	public Color get() {
		return Display.getDefault().getSystemColor(styleBit);
	}

}
