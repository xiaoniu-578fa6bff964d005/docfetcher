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

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
final class HotkeyDialog {
	
	// TODO post-release-1.1: macosx: On Mac maybe use Command key instead of Control key
	
	private final Shell shell;
	private final Text hotkeyBox;
	private final int[] initialHotkey = SettingsConf.IntArray.HotkeyToFront.get();
	@NotNull private int[] hotkey = initialHotkey;
	
	public HotkeyDialog(@NotNull Shell parentShell) {
		shell = new Shell(parentShell, SWT.RESIZE | SWT.CLOSE | SWT.TITLE);
		shell.setLayout(new FormLayout());
		shell.setImage(Img.LETTERS.get());
		shell.setText("keybox_title");
		
		Label keyLabel = new Label(shell, SWT.NONE);
		keyLabel.setText("keybox_msg");
		hotkeyBox = new Text(shell, SWT.BORDER | SWT.READ_ONLY);
		hotkeyBox.setText(toString(hotkey));
		
		// On Windows XP, the read-only text field looks like a label without this
		hotkeyBox.setBackground(Col.LIST_BACKGROUND.get());
		hotkeyBox.setForeground(Col.LIST_FOREGROUND.get());
		
		Label vSpacer = new Label(shell, SWT.NONE);
		Label sep = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		Label hSpacer = new Label(shell, SWT.NONE);
		
		Button okBt = new Button(shell, SWT.PUSH);
		okBt.setText("&OK");
		Button cancelBt = new Button(shell, SWT.PUSH);
		cancelBt.setText("&Cancel");
		Button restoreBt = new Button(shell, SWT.PUSH);
		restoreBt.setText("&Restore Default");
		
		boolean leftAlign = shell.getDisplay().getDismissalAlignment() == SWT.LEFT;
		Button leftBt = leftAlign ? okBt : cancelBt;
		Button rightBt = leftAlign ? cancelBt : okBt;
		
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top(0, 10).left(0, 10).right(100, -10).applyTo(keyLabel);
		fdf.top(keyLabel, 10).left(0, 10).right(100, -10).applyTo(hotkeyBox);
		fdf.reset().minWidth(75).bottom().right().applyTo(rightBt);
		fdf.right(rightBt).applyTo(leftBt);
		fdf.right(leftBt).applyTo(restoreBt);
		fdf.minWidth(0).right(restoreBt).left().applyTo(hSpacer);
		fdf.reset().left().right().bottom(rightBt).applyTo(sep);
		fdf.bottom(sep).top(hotkeyBox).applyTo(vSpacer);
		
		hotkeyBox.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int[] newHotkey = HotkeyDialog.this.acceptHotkey(e.stateMask, e.keyCode);
				if (newHotkey == null)
					return;
				hotkey = newHotkey;
				hotkeyBox.setText(HotkeyDialog.toString(hotkey));
			}
		});
		
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				hotkey = initialHotkey;
			}
		});
		
		restoreBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hotkey = SettingsConf.IntArray.HotkeyToFront.defaultValue;
				hotkeyBox.setText(HotkeyDialog.toString(hotkey));
			}
		});
		
		okBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				 * Don't call shell.close() here, we don't want to reset the
				 * hotkey.
				 */
				SettingsConf.IntArray.HotkeyToFront.set(hotkey);
				shell.dispose();
			}
		});
		
		cancelBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
	}
	
	@NotNull
	public int[] open() {
		// We don't want the current hotkey to interfere with the hotkey dialog
		boolean hotkeyEnabled = SettingsConf.Bool.HotkeyEnabled.get();
		SettingsConf.Bool.HotkeyEnabled.set(false);
		
		Util.setCenteredBounds(shell);
		shell.open();
		hotkeyBox.setFocus();
		while (!shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		
		SettingsConf.Bool.HotkeyEnabled.set(hotkeyEnabled);
		return hotkey;
	}
	
	/**
	 * Returns the hotkey if the given SWT state mask and keycode represent
	 * valid user input, otherwise returns null. Some user inputs are invalid
	 * because they wouldn't work as global hotkeys.
	 */
	@Nullable
	private int[] acceptHotkey(int stateMask, int keyCode) {
		int[] hotkey = new int[] { stateMask, keyCode };
		
		// State mask must be SWT.NONE or a combination of SWT.CTRL, SWT.ALT and SWT.SHIFT
		if ((stateMask & (SWT.CTRL | SWT.ALT | SWT.SHIFT)) == 0)
			return null;

		// Accept special keys
		switch (keyCode) {
		case SWT.F1:
		case SWT.F2:
		case SWT.F3:
		case SWT.F4:
		case SWT.F5:
		case SWT.F6:
		case SWT.F7:
		case SWT.F8:
		case SWT.F9:
		case SWT.F10:
		case SWT.F11:
		case SWT.F12:
		case SWT.PAUSE:
		case SWT.PRINT_SCREEN:
		case SWT.BS:
		case SWT.CR:
		case SWT.INSERT:
		case SWT.DEL:
		case SWT.HOME:
		case SWT.END:
		case SWT.PAGE_UP:
		case SWT.PAGE_DOWN:
		case SWT.ARROW_UP:
		case SWT.ARROW_DOWN:
		case SWT.ARROW_LEFT:
		case SWT.ARROW_RIGHT: return hotkey;
		default: break;
		}
		
		// Shift and Alt as keycode are not allowed
		if (keyCode == SWT.SHIFT || keyCode == SWT.ALT)
			return null;
		
		// Accept letters and digits
		if (Character.isLetterOrDigit(keyCode)) {
			if (Character.isLetter(keyCode)) // return uppercase letters
				hotkey[1] = Character.toUpperCase(hotkey[1]);
			return hotkey;
		}
		
		return null; // user input rejected
	}
	
	/**
	 * Returns a string representing the key combination, e.g. "CTRL + H".
	 */
	@NotNull
	static String toString(@NotNull int[] hotkey) {
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
