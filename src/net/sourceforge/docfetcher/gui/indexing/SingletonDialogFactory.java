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

import net.sourceforge.docfetcher.gui.indexing.SingletonDialogFactory.Dialog;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public abstract class SingletonDialogFactory<D extends Dialog> {

	public static void main(String[] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setText("Main Shell");
		shell.setLayout(new GridLayout());
		Util.setCenteredBounds(shell, 200, 200);

		class MyDialog implements Dialog {
			private final Shell shell;

			public MyDialog(Shell shell) {
				this.shell = shell;
			}

			public Shell getShell() {
				return shell;
			}
		}

		final SingletonDialogFactory<Dialog> factory = new SingletonDialogFactory<Dialog>(
				shell) {
			protected Dialog createDialog(Shell parentShell) {
				Shell shell2 = new Shell(parentShell, SWT.SHELL_TRIM);
				shell2.setText("Sub-Shell");
				shell2.setSize(200, 200);
				Point parentLocation = shell.getLocation();
				shell2.setLocation(parentLocation.x + 50, parentLocation.y + 50);
				shell2.setLayout(new GridLayout());

				Label label = new Label(shell2, SWT.NONE);
				label.setText("Label");
				label.setLayoutData(new GridData(
					SWT.CENTER, SWT.CENTER, true, true));

				return new MyDialog(shell2);
			}
		};

		Button bt = new Button(shell, SWT.PUSH);
		bt.setText("Open");
		bt.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		bt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				factory.open();
			}
		});

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
		display.dispose();
	}

	public interface Dialog {
		@NotNull
		public Shell getShell();
	}
	
	public final Event<Void> evtDialogOpened = new Event<Void>();

	private final Shell parentShell;
	@Nullable private D dialog;

	public SingletonDialogFactory(@NotNull Shell parentShell) {
		this.parentShell = Util.checkNotNull(parentShell);
	}

	@NotNull
	public final D open() {
		if (dialog != null) {
			dialog.getShell().open();
			evtDialogOpened.fire(null);
			return dialog;
		}

		dialog = createDialog(parentShell);
		Shell shell = dialog.getShell();
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dialog = null;
			}
		});
		shell.open();
		evtDialogOpened.fire(null);
		return dialog;
	}

	@NotNull
	protected abstract D createDialog(@NotNull Shell parentShell);

}
