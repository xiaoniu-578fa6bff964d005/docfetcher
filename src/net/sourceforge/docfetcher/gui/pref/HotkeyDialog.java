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
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.UtilGui;
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
		shell.setText(Msg.keybox_title.get());
		
		Label keyLabel = new Label(shell, SWT.NONE);
		keyLabel.setText(Msg.keybox_msg.get());
		hotkeyBox = new Text(shell, SWT.BORDER | SWT.READ_ONLY);
		hotkeyBox.setText(UtilGui.toString(hotkey));
		
		// On Windows XP, the read-only text field looks like a label without this
		hotkeyBox.setBackground(Col.LIST_BACKGROUND.get());
		hotkeyBox.setForeground(Col.LIST_FOREGROUND.get());
		
		Label vSpacer = new Label(shell, SWT.NONE);
		Label sep = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		Label hSpacer = new Label(shell, SWT.NONE);
		
		Button okBt = new Button(shell, SWT.PUSH);
		okBt.setText(Msg.ok.get());
		Button cancelBt = new Button(shell, SWT.PUSH);
		cancelBt.setText(Msg.cancel.get());
		Button restoreBt = new Button(shell, SWT.PUSH);
		restoreBt.setText(Msg.restore_default.get());
		
		boolean leftAlign = shell.getDisplay().getDismissalAlignment() == SWT.LEFT;
		Button leftBt = leftAlign ? okBt : cancelBt;
		Button rightBt = leftAlign ? cancelBt : okBt;
		
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top(0, 10).left(0, 10).right(100, -10).applyTo(keyLabel);
		fdf.top(keyLabel, 10).left(0, 10).right(100, -10).applyTo(hotkeyBox);
		fdf.reset().minWidth(Util.BTW).bottom().right().applyTo(rightBt);
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
				hotkeyBox.setText(UtilGui.toString(hotkey));
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
				hotkeyBox.setText(UtilGui.toString(hotkey));
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

}
