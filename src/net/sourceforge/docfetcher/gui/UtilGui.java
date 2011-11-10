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

import net.sourceforge.docfetcher.enums.Msg;
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
		
		Util.runSwtSafe(control, new Runnable() {
			public void run() {
				String url = ManualLocator.getMemoryLimitPageUrl();
				
				// Note: getShell() must be called in the SWT thread
				InfoDialog dialog = new InfoDialog(control.getShell());
				dialog.setTitle(Msg.out_of_memory.get());
				dialog.setImage(SWT.ICON_ERROR);
				dialog.setText(Msg.out_of_memory_instructions.format(url));
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
		String key = "";
		
		switch (keyCode) {
		case SWT.F1: key = Msg.f1.get(); break;
		case SWT.F2: key = Msg.f2.get(); break;
		case SWT.F3: key = Msg.f3.get(); break;
		case SWT.F4: key = Msg.f4.get(); break;
		case SWT.F5: key = Msg.f5.get(); break;
		case SWT.F6: key = Msg.f6.get(); break;
		case SWT.F7: key = Msg.f7.get(); break;
		case SWT.F8: key = Msg.f8.get(); break;
		case SWT.F9: key = Msg.f9.get(); break;
		case SWT.F10: key = Msg.f10.get(); break;
		case SWT.F11: key = Msg.f11.get(); break;
		case SWT.F12: key = Msg.f12.get(); break;
		case SWT.PAUSE: key = Msg.pause_key.get(); break;
		case SWT.PRINT_SCREEN: key = Msg.print_screen_key.get(); break;
		case SWT.BS: key = Msg.backspace_key.get(); break;
		case SWT.CR: key = Msg.enter_key.get(); break;
		case SWT.INSERT: key = Msg.insert_key.get(); break;
		case SWT.DEL: key = Msg.delete_key.get(); break;
		case SWT.HOME: key = Msg.home_key.get(); break;
		case SWT.END: key = Msg.end_key.get(); break;
		case SWT.PAGE_UP: key = Msg.page_up_key.get(); break;
		case SWT.PAGE_DOWN: key = Msg.page_down_key.get(); break;
		case SWT.ARROW_UP: key = Msg.arrow_up.get(); break;
		case SWT.ARROW_DOWN: key = Msg.arrow_down.get(); break;
		case SWT.ARROW_LEFT: key = Msg.arrow_left.get(); break;
		case SWT.ARROW_RIGHT: key = Msg.arrow_right.get(); break;
		default: {
			key = String.valueOf((char) keyCode).toUpperCase();
		}
		}
		
		if (alt) key = Msg.alt_key.get() + " + " + key;
		if (shift) key = Msg.shift_key.get() + " + " + key;
		if (ctrl) key = Msg.ctrl_key.get() + " + " + key;
		return key;
	}
	
}
