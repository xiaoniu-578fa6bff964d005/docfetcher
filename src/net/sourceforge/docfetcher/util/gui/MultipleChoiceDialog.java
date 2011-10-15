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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
public final class MultipleChoiceDialog<A> {

	public static void main(String[] args) {
		Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Util.setCenteredBounds(shell, 150, 75);

		Button bt = new Button(shell, SWT.PUSH);
		bt.setText("Open Dialog");
		bt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MultipleChoiceDialog<String> dialog = new MultipleChoiceDialog<String>(shell);
				dialog.setTitle("Choose Wisely");
				dialog.setImage(SWT.ICON_QUESTION);
				dialog.setText("Which way?");
				dialog.addButton("Door 1", "Door 1 chosen.");
				dialog.addButton("Door 2", "Door 2 chosen.");
				dialog.addButton("Door 3", "Door 3 chosen.");
				dialog.addButton("Door 4", "Door 4 chosen.");
				Object answer = dialog.open();
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

	private final Shell shell;
	private final Label icon;
	private final StyledText label;
	
	private final GridLayout gridLayout;
	private final GridData gridData;
	
	@Nullable private A answer;

	public MultipleChoiceDialog(@NotNull Shell parent) {
		Util.checkNotNull(parent);
		shell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM);
		gridLayout = Util.createGridLayout(0, true, 10, 10);
		shell.setLayout(gridLayout);

		Composite labelComp = new Composite(shell, SWT.NONE);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 0, 1);
		labelComp.setLayoutData(gridData);
		labelComp.setLayout(Util.createGridLayout(2, false, 10, 15));
		
		icon = new Label(labelComp, SWT.NONE);
		icon.setImage(shell.getDisplay().getSystemImage(SWT.ICON_QUESTION));
		icon.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		label = new StyledText(labelComp, SWT.WRAP | SWT.READ_ONLY);
		label.setEnabled(false);
		label.setBackground(icon.getBackground());
		label.setForeground(icon.getForeground()); // Not necessary
		GridData labelData = new GridData(SWT.FILL, SWT.FILL, true, true);
		labelData.widthHint = 250;
		label.setLayoutData(labelData);
	}
	
	public void setTitle(@NotNull String title) {
		Util.checkNotNull(title);
		shell.setText(title);
	}
	
	public void setImage(@NotNull Image image) {
		Util.checkNotNull(image);
		icon.setImage(image);
	}
	
	public void setImage(int swtImage) {
		icon.setImage(shell.getDisplay().getSystemImage(swtImage));
	}
	
	public void setText(@NotNull String text) {
		Util.checkNotNull(text);
		label.setText(text);
	}
	
	@Nullable
	public A addButton(@NotNull final String text, @Nullable final A answer) {
		Util.checkNotNull(text);
		Button bt = new Button(shell, SWT.PUSH);
		bt.setText(text);
		bt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		bt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MultipleChoiceDialog.this.answer = answer;
				shell.close();
			}
		});
		gridLayout.numColumns += 1;
		gridData.horizontalSpan += 1;
		return answer;
	}

	@Nullable
	public A open() {
		Util.setCenteredBounds(shell);
		shell.open();
		while (!shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		return answer;
	}

}
