/*******************************************************************************
 * Copyright (c) 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.base.gui;

import net.sourceforge.docfetcher.base.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A generic input dialog widget.
 * 
 * @author Tran Nam Quang
 */
public class InputDialog {
	
	// TODO i18n
	public static int BUTTON_WIDTH = 75;
	public static String OK = "&OK";
	public static String CANCEL = "&Cancel";
	
	private Shell shell;
	private String answer;
	private Combo text;
	private boolean selectFilenameOnly = false;
	
	/**
	 * Creates an input dialog instance. The dialog remains invisible until
	 * {@link #open()} is called.
	 * 
	 * @param parent
	 *            The parent shell.
	 * @param title
	 *            The window title for this input dialog.
	 * @param msg
	 *            The message to display on this input dialog.
	 * @param defaultValue
	 *            The default value for the input field.
	 */
	public InputDialog(Shell parent, String title, String msg, String defaultValue) {
		shell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
		shell.setText(title);
		
		Label label = new Label(shell, SWT.NONE);
		text = new Combo(shell, SWT.BORDER | SWT.SINGLE);
		Util.selectAllOnFocus(text);
		Label fillerLabel = new Label(shell, SWT.NONE);
		Label separator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		Button okBt = new Button(shell, SWT.PUSH);
		Button cancelBt = new Button(shell, SWT.PUSH);
		
		label.setText(msg);
		text.setText(defaultValue);
		text.setSelection(new Point(0, text.getText().length()));
		okBt.setText(OK);
		cancelBt.setText(CANCEL);
		
		boolean leftAlign = shell.getDisplay().getDismissalAlignment() == SWT.LEFT;
		Button leftBt = leftAlign ? okBt : cancelBt;
		Button rightBt = leftAlign ? cancelBt : okBt;
		
		shell.setLayout(Util.createFormLayout(5));
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top().left().right().applyTo(label);
		fdf.top(label).applyTo(text);
		fdf.reset().minWidth(BUTTON_WIDTH).bottom().right().applyTo(rightBt);
		fdf.right(rightBt).applyTo(leftBt);
		fdf.reset().left().right().bottom(rightBt).applyTo(separator);
		fdf.top(text).bottom(separator).applyTo(fillerLabel);
		
		okBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				answer = text.getText();
				shell.close();
			}
		});
		
		cancelBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					answer = text.getText();
					shell.close();
				}
			}
		});
	}
	
	/**
	 * @see #setSelectFilenameOnly(boolean)
	 */
	public boolean isSelectFilenameOnly() {
		return selectFilenameOnly;
	}

	/**
	 * If a filename has been set as the default value for this input dialog,
	 * calling this method will set the initial selection to the filename only,
	 * excluding the file extension.
	 */
	public void setSelectFilenameOnly(boolean selectFilenameOnly) {
		this.selectFilenameOnly = selectFilenameOnly;
	}

	/**
	 * Sets the items in the drop-down list of the dialog.
	 */
	public void setHistory(String[] history) {
		String str = text.getText();
		text.setItems(history);
		text.setText(str);
	}
	
	/**
	 * Opens the dialog and returns the input string or null if the dialog was
	 * canceled.
	 */
	public String open() {
		Util.setCenteredMinBounds(shell, 300, 150);
		shell.open();
		if (selectFilenameOnly) {
			String s = text.getText();
			int index = s.lastIndexOf('.');
			if (index != -1)
				text.setSelection(new Point(0, index));
			else
				text.setSelection(new Point(0, text.getText().length()));
		} else {
			text.setSelection(new Point(0, text.getText().length()));
		}
		while (! shell.isDisposed()) {
			if (! shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		return answer;
	}

}
