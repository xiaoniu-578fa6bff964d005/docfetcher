/*******************************************************************************
 * Copyright (c) 2012 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.util.gui.dialog;

import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
public final class ListConfirmDialog {

	public static void main(String[] args) {
		Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		Image img1 = display.getSystemImage(SWT.ICON_WARNING);
		final Image img = new Image(display, img1.getImageData().scaledTo(16, 16));

		Util.createPushButton(shell, "Open Dialog", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ListConfirmDialog dialog = new ListConfirmDialog(shell, SWT.ICON_INFORMATION);
				dialog.setTitle("Title");
				dialog.setText("This is a message containing a <a href=\"test\">hyperlink</a>.");
				
				for (int i = 0; i < 10; i++) {
					dialog.addItem(img, "Item " + i);
				}
				
				dialog.evtLinkClicked.add(new Event.Listener<String>() {
					public void update(String eventData) {
						Util.println("Link clicked: " + eventData);
					}
				});
				
				dialog.open();
			}
		});

		Util.setCenteredBounds(shell, 200, 100);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		img.dispose();
	}
	
	public final Event<String> evtLinkClicked = new Event<String>();
	
	private final Shell shell;
	private final Link textControl;
	private final Table table;
	private final Button okBt;
	private final Button cancelBt;
	
	private boolean answerOK = false;
	
	public ListConfirmDialog(@NotNull Shell parent, int iconConstant) {
		shell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);

		Composite infoComp = new Composite(shell, SWT.NONE);
		infoComp.setLayout(Util.createGridLayout(2, false, 0, 5));
		
		Label icon = new Label(infoComp, SWT.NONE);
		icon.setImage(shell.getDisplay().getSystemImage(iconConstant));
		icon.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
		
		textControl = new Link(infoComp, SWT.NONE);
		textControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		table = new Table(shell, SWT.BORDER);
		
		okBt = Util.createPushButton(shell, AppUtil.Messages.ok.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				answerOK = true;
				shell.close();
			}
		});
		
		cancelBt = Util.createPushButton(shell, AppUtil.Messages.cancel.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		
		textControl.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtLinkClicked.fire(e.text);
			}
		});
		
		Button[] okCancelBts = Util.maybeSwapButtons(okBt, cancelBt);
		
		shell.setLayout(Util.createFormLayout(5));
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top().left().right().applyTo(infoComp);
		fdf.reset().minWidth(Util.BTW).bottom().right().applyTo(okCancelBts[1]);
		fdf.right(okCancelBts[1]).applyTo(okCancelBts[0]);
		fdf.reset().left().right().top(infoComp, 10).bottom(okCancelBts[1], -10).applyTo(table);
	}
	
	public void setTitle(@NotNull String title) {
		shell.setText(title);
	}
	
	/**
	 * Sets the dialog message. Hyperlinks are supported.
	 */
	public void setText(@NotNull String text) {
		textControl.setText(text);
	}
	
	public void addItem(@Nullable Image icon, @NotNull String text) {
		TableItem item = new TableItem(table, SWT.NONE);
		if (icon != null)
			item.setImage(icon);
		item.setText(text);
	}
	
	public void setButtonLabels(@NotNull String okLabel, @NotNull String cancelLabel) {
		okBt.setText(okLabel);
		cancelBt.setText(cancelLabel);
	}
	
	/**
	 * Opens the dialog and returns whether the OK button was clicked.
	 */
	public boolean open() {
		Util.setCenteredBounds(shell, 350, 350);
		shell.open();
		table.setFocus(); // move focus away from hyperlinks
		while (! shell.isDisposed()) {
			if (! shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		return answerOK;
	}

}
