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

package net.sourceforge.docfetcher.util.gui.dialog;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.AppUtil.Messages;
import net.sourceforge.docfetcher.util.errorreport.SentryHandler;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

public class StackTraceWindow {
	
	public static int windowCount = 0;
	
	private Shell shell;
	private Link label;
	private Text text;
	private Button button;

	private boolean reportOnline;

	private Throwable throwable;

	public StackTraceWindow(Display display) {
		shell = new Shell(display, SWT.PRIMARY_MODAL | SWT.SHELL_TRIM | SWT.RESIZE);
		shell.setText(Messages.system_error.get()); // Default shell title
		
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

		button = new Button(shell, SWT.CHECK);
		button.setText(Msg.report_bug_online.get());
		button.setSelection(true);

		shell.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(10).left().right().top().applyTo(label);
		fdf.reset().left().right().bottom().applyTo(button);
		fdf.top(label).bottom(button).applyTo(text);

		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				reportOnline=button.getSelection();

				if(reportOnline && throwable!=null)
                    SentryHandler.capture(throwable);
			}
		});
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
		windowCount++;
		while (! shell.isDisposed()) {
			if (! shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		windowCount--;
	}

	public boolean isReportOnline() {
		return reportOnline;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

}
