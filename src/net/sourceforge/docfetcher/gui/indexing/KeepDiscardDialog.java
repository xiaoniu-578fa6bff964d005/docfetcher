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

package net.sourceforge.docfetcher.gui.indexing;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
final class KeepDiscardDialog {

	public static void main(String[] args) {
		Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Util.setCenteredBounds(shell, 150, 75);

		Button bt = new Button(shell, SWT.PUSH);
		bt.setText("Open Dialog");
		bt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				KeepDiscardDialog dialog = new KeepDiscardDialog(shell);
				Answer answer = dialog.open();
				Util.println(answer);
			}
		});

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	public enum Answer {
		KEEP, DISCARD, CONTINUE
	}

	private final Shell shell;
	@NotNull private Answer answer = Answer.CONTINUE;

	public KeepDiscardDialog(@NotNull Shell parent) {
		Util.checkNotNull(parent);
		shell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM);
		shell.setText("Abort Indexing?"); // TODO i18n
		shell.setLayout(Util.createGridLayout(3, true, 10, 10));

		Composite labelComp = new Composite(shell, SWT.NONE);
		labelComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		labelComp.setLayout(Util.createGridLayout(2, false, 10, 15));
		
		Label icon = new Label(labelComp, SWT.NONE);
		icon.setImage(shell.getDisplay().getSystemImage(SWT.ICON_QUESTION));
		icon.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		StyledText label = new StyledText(labelComp, SWT.WRAP | SWT.READ_ONLY);
		// TODO i18n
		String labelText = "You are about to abort an indexing process. Do you want to keep the"
				+ " index created so far? Keeping it allows you to continue indexing later by running an index update.";
		label.setText(labelText);
		label.setEnabled(false);
		label.setBackground(icon.getBackground());
		label.setForeground(icon.getForeground()); // Not necessary
		GridData labelData = new GridData(SWT.FILL, SWT.FILL, true, true);
		labelData.widthHint = 250;
		label.setLayoutData(labelData);
		
		// TODO i18n
		createButton("&Keep", Answer.KEEP);
		createButton("&Discard", Answer.DISCARD);
		createButton("Don't &Abort", Answer.CONTINUE);
	}
	
	private void createButton(@NotNull String text, @NotNull final Answer answer) {
		Button bt = new Button(shell, SWT.PUSH);
		bt.setText(text);
		bt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		bt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				KeepDiscardDialog.this.answer = answer;
				shell.close();
			}
		});
	}

	@NotNull
	public Answer open() {
		Util.setCenteredBounds(shell);
		shell.open();
		while (!shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		return answer;
	}

}
