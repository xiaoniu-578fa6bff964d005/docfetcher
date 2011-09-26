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

package net.sourceforge.docfetcher.gui.filter;

import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
public final class FilesizePanel {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(Util.createGridLayout(1, false, 5, 5));

		final FilesizePanel filesizePanel = new FilesizePanel(shell);
		filesizePanel.getControl().setLayoutData(
			new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label sep = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Label label = new Label(shell, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		filesizePanel.evtValuesChanged.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				Long[] values = filesizePanel.getValuesInKB();
				label.setText("Changed: " + values[0] + ", " + values[1]);
			}
		});

		Util.setCenteredBounds(shell);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	public final Event<Void> evtValuesChanged = new Event<Void>();

	/*
	 * Cached values of the minimum/maximum filesize. They are marked volatile
	 * for access by another thread.
	 */
	@Nullable
	private volatile Long minBytes;
	@Nullable
	private volatile Long maxBytes;

	private final Composite comp;
	private final Text minField;
	private final Combo minCombo;
	private final Text maxField;
	private final Combo maxCombo;

	public FilesizePanel(@NotNull Composite parent) {
		comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new FormLayout());
		UtilGui.paintBorder(comp);

		int textStyle = SWT.RIGHT | SWT.SINGLE | SWT.BORDER;
		int comboStyle = SWT.DROP_DOWN | SWT.READ_ONLY;

		minField = new Text(comp, textStyle);
		minCombo = new Combo(comp, comboStyle);
		maxField = new Text(comp, textStyle);
		maxCombo = new Combo(comp, comboStyle);

		// Layout
		FormDataFactory fdf = FormDataFactory.getInstance();
		int m = FormDataFactory.DEFAULT_MARGIN;
		fdf.top().bottom().right(50, -m).applyTo(minCombo);
		fdf.right(minCombo, -m / 2).left().applyTo(minField);
		fdf.reset().top().bottom().right().applyTo(maxCombo);
		fdf.right(maxCombo, -m / 2).left(50, m).applyTo(maxField);

		// Ensure the user can only enter non-negative integers into the
		// textboxes
		VerifyListener numbersOnlyListener = new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				e.doit = e.text.matches("[0-9]*"); //$NON-NLS-1$
			}
		};
		minField.addVerifyListener(numbersOnlyListener);
		maxField.addVerifyListener(numbersOnlyListener);

		// Set the contents of the combo widgets
		String[] comboItems = FilesizeUnit.valuesAsStrings();
		minCombo.setItems(comboItems);
		maxCombo.setItems(comboItems);
		minCombo.select(FilesizeUnit.KB.ordinal());
		maxCombo.select(FilesizeUnit.KB.ordinal());

		// Redirect modification events
		class Redirector extends SelectionAdapter implements ModifyListener {
			public void modifyText(ModifyEvent e) {
				handleValuesChanged();
			}

			public void widgetSelected(SelectionEvent e) {
				handleValuesChanged();
			}
		}
		Redirector redirector = new Redirector();
		minField.addModifyListener(redirector);
		maxField.addModifyListener(redirector);
		minCombo.addSelectionListener(redirector);
		maxCombo.addSelectionListener(redirector);
	}

	@NotNull
	public Control getControl() {
		return comp;
	}

	private void handleValuesChanged() {
		minBytes = parse(minField, minCombo);
		maxBytes = parse(maxField, maxCombo);
		evtValuesChanged.fire(null);
	}

	// Returns bytes, or null if the values are invalid
	@Nullable
	private Long parse(@NotNull Text text, @NotNull Combo combo) {
		try {
			long size = Long.parseLong(text.getText());
			FilesizeUnit unit = FilesizeUnit.valueOf(combo.getText());
			return FilesizeUnit.Byte.convert(size, unit);
		}
		catch (Exception e) {
			return null;
		}
	}
	
	// Returned array has size 2 and may contain null values.
	@Nullable
	public Long[] getValuesInKB() {
		Long minKB = FilesizeUnit.KB.convert(minBytes, FilesizeUnit.Byte);
		Long maxKB = FilesizeUnit.KB.convert(maxBytes, FilesizeUnit.Byte);
		if (minKB == null && maxKB == null)
			return null;
		return new Long[] { minKB, maxKB };
	}

}
