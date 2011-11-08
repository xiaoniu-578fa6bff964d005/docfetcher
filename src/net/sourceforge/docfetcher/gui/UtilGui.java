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

package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SettingsConf.FontDescription;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.dialog.InfoDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class UtilGui {
	
	private UtilGui() {
	}
	
	static final int OPEN_LIMIT = 10;
	
	public static final int DIALOG_STYLE = SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM | SWT.MIN | SWT.MAX | SWT.RESIZE;
	
	/**
	 * Paints a border around the given Control. This can be used as a
	 * replacement for the ugly native border of Composites with SWT.BORDER
	 * style on Windows with classic theme turned on.
	 */
	public static void paintBorder(final Control control) {
		control.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				Point size = control.getSize();
				e.gc.setForeground(Col.WIDGET_NORMAL_SHADOW.get());
				e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);
				e.gc.setForeground(Col.WHITE.get());
				e.gc.drawRectangle(1, 1, size.x - 3, size.y - 3);
			}
		});
	}

	/**
	 * Attaches a focus listener to the given StyledText that clears the
	 * selection and resets the caret position when the StyledText looses focus.
	 */
	public static void clearSelectionOnFocusLost(@NotNull final StyledText styledText) {
		styledText.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				styledText.setSelection(0);
			}
		});
	}
	
	public static void setGridData(	@NotNull Control control,
									boolean grabExcessVerticalSpace) {
		control.setLayoutData(new GridData(
			SWT.FILL, SWT.FILL, true, grabExcessVerticalSpace));
	}
	
	public static void showOutOfMemoryMessage(	@NotNull final Control control,
												@NotNull CheckedOutOfMemoryError e) {
		Util.printErr(e.getOutOfMemoryError());
		
		// TODO pre-release: Insert link to manual page
		Util.runSwtSafe(control, new Runnable() {
			public void run() {
				// Note: getShell() must be called in the SWT thread
				InfoDialog dialog = new InfoDialog(control.getShell());
				dialog.setTitle("Out Of Memory");
				dialog.setImage(SWT.ICON_ERROR);
				dialog.setText("DocFetcher has run out of memory. " +
						"Please see the relevant <a href=\"www.google.com\">manual page</a> for further instructions.");
				dialog.open();
			}
		});
	}
	
	@NotNull
	public static FontDescription getPreviewFontNormal() {
		if (Util.IS_WINDOWS)
			return SettingsConf.FontDescription.PreviewWindows;
		if (Util.IS_LINUX)
			return SettingsConf.FontDescription.PreviewLinux;
		if (Util.IS_MAC_OS_X)
			return SettingsConf.FontDescription.PreviewMacOsX;
		throw new IllegalStateException();
	}
	
	@NotNull
	public static FontDescription getPreviewFontMono() {
		if (Util.IS_WINDOWS)
			return SettingsConf.FontDescription.PreviewMonoWindows;
		if (Util.IS_LINUX)
			return SettingsConf.FontDescription.PreviewMonoLinux;
		if (Util.IS_MAC_OS_X)
			return SettingsConf.FontDescription.PreviewMonoMacOsX;
		throw new IllegalStateException();
	}

	/**
	 * Returns a string representing the key combination, e.g. "CTRL + H".
	 */
	@NotNull
	public static String toString(@NotNull int[] hotkey) {
		int stateMask = hotkey[0];
		int keyCode = hotkey[1];
		boolean ctrl = (stateMask & SWT.CTRL) != 0;
		boolean shift = (stateMask & SWT.SHIFT) != 0;
		boolean alt = (stateMask & SWT.ALT) != 0;
		String key = ""; //$NON-NLS-1$
		
		switch (keyCode) {
		case SWT.F1: key = "F1"; break; //$NON-NLS-1$
		case SWT.F2: key = "F2"; break; //$NON-NLS-1$
		case SWT.F3: key = "F3"; break; //$NON-NLS-1$
		case SWT.F4: key = "F4"; break; //$NON-NLS-1$
		case SWT.F5: key = "F5"; break; //$NON-NLS-1$
		case SWT.F6: key = "F6"; break; //$NON-NLS-1$
		case SWT.F7: key = "F7"; break; //$NON-NLS-1$
		case SWT.F8: key = "F8"; break; //$NON-NLS-1$
		case SWT.F9: key = "F9"; break; //$NON-NLS-1$
		case SWT.F10: key = "F10"; break; //$NON-NLS-1$
		case SWT.F11: key = "F11"; break; //$NON-NLS-1$
		case SWT.F12: key = "F12"; break; //$NON-NLS-1$
		case SWT.PAUSE: key = "Pause"; break; //$NON-NLS-1$
		case SWT.PRINT_SCREEN: key = "Print Screen"; break; //$NON-NLS-1$
		case SWT.BS: key = "Backspace"; break; //$NON-NLS-1$
		case SWT.CR: key = "Enter"; break; //$NON-NLS-1$
		case SWT.INSERT: key = "Insert"; break; //$NON-NLS-1$
		case SWT.DEL: key = "Delete"; break; //$NON-NLS-1$
		case SWT.HOME: key = "Home"; break; //$NON-NLS-1$
		case SWT.END: key = "End"; break; //$NON-NLS-1$
		case SWT.PAGE_UP: key = "Page Up"; break; //$NON-NLS-1$
		case SWT.PAGE_DOWN: key = "Page Down"; break; //$NON-NLS-1$
		case SWT.ARROW_UP: key = "Arrow Up"; break; //$NON-NLS-1$
		case SWT.ARROW_DOWN: key = "Arrow Down"; break; //$NON-NLS-1$
		case SWT.ARROW_LEFT: key = "Arrow Left"; break; //$NON-NLS-1$
		case SWT.ARROW_RIGHT: key = "Arrow Right"; break; //$NON-NLS-1$
		default: {
			key = String.valueOf((char) keyCode).toUpperCase();
		}
		}
		
		if (alt) key = "Alt + " + key; //$NON-NLS-1$
		if (shift) key = "Shift + " + key; //$NON-NLS-1$
		if (ctrl) key = "Ctrl + " + key; //$NON-NLS-1$
		return key;
	}
	
}
