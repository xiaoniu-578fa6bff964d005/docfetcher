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

package net.sourceforge.docfetcher.base.gui;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.AppUtil.Msg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class StackTraceWindow {
	
	private Shell shell;
	private Link label;
	private Text text;
	
	public StackTraceWindow(Display display) {
		shell = new Shell(display, SWT.PRIMARY_MODAL | SWT.SHELL_TRIM | SWT.RESIZE);
		shell.setText(Msg.system_error.get()); // Default shell title
		
		label = new Link(shell, SWT.NONE);
		label.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Util.launch(e.text);
			}
		});
		
		text = new Text(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setBackground(Col.WHITE.get());
		text.setForeground(Col.RED.get());
		text.setFocus();
		
		shell.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(10).left().right().top().applyTo(label);
		fdf.top(label).bottom().applyTo(text);
	}
	
	public void setTitle(String title) {
		shell.setText(title);
	}
	
	public void setText(String textWithLinks) {
		label.setText(textWithLinks);
	}
	
	public void setTitleImage(Image image) {
		shell.setImage(image);
	}
	
	public void setStackTrace(String stackTrace) {
		text.setText(stackTrace);
	}
	
	public void open() {
		Util.setCenteredBounds(shell, 400, 400); // Don't store shell size
		shell.open();
		while (! shell.isDisposed()) {
			if (! shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
	}

}
