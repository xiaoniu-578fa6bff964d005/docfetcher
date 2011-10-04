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

package net.sourceforge.docfetcher.util.gui.viewer;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.viewer.ColumnEditSupport.ComboEditSupport;
import net.sourceforge.docfetcher.util.gui.viewer.ColumnEditSupport.TextEditSupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
abstract class TableEditSupport<E> {
	
	private final Table table;
	private final TableEditor tableEditor;

	public TableEditSupport(@NotNull final Table table) {
		Util.checkNotNull(table);
		this.table = table;
		tableEditor = new TableEditor(table);
		tableEditor.grabHorizontal = true;
		tableEditor.grabVertical = true;
		
		table.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			public void mouseDown(MouseEvent e) {
				cancelEditing();
				
				final TableItem item = table.getItem(new Point(e.x, e.y));
				if (item == null)
					return;
				
				// Determine the column that was clicked
				int columnIndex = -1;
				for (int i = 0; i < table.getColumnCount(); i++) {
					if (item.getBounds(i).contains(e.x, e.y)) {
						columnIndex = i;
						break;
					}
				}
				if (columnIndex == -1)
					return;
				
				// Create and initialize cell editor
				ColumnEditSupport<E> editSupport = getColumnEditSupport(columnIndex);
				if (editSupport == null)
					return; // Return if the column doesn't support editing
				final Control editor;
				if (editSupport instanceof TextEditSupport) {
					TextEditSupport<E> textEditSupport = (TextEditSupport<E>) editSupport;
					editor = createTextEditor(textEditSupport, item, columnIndex);
				}
				else if (editSupport instanceof ComboEditSupport) {
					ComboEditSupport<E, ?> comboEditSupport = (ComboEditSupport<E, ?>) editSupport;
					editor = createComboEditor(comboEditSupport, item, columnIndex);
				}
				else {
					throw new IllegalStateException();
				}
				
				tableEditor.setEditor(editor, item, columnIndex);
				editor.setFocus();
			}
		});
	}
	
	@NotNull
	private Control createTextEditor(	@NotNull final TextEditSupport<E> editSupport,
										@NotNull final TableItem item,
										final int columnIndex) {
		final Composite comp = new Composite(table, SWT.NONE);
		comp.setLayout(Util.createGridLayout(1, false, 0, 0));
		
		String text = item.getText(columnIndex);
		final StyledText editor = new StyledText(comp, SWT.SINGLE);
		editor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		editor.setText(text);
		editor.setSelection(text.length());
		editor.selectAll();
		
		final Runnable saver = new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				E element = (E) item.getData();
				String newText = editor.getText();
				editSupport.setText(element, newText);
				item.setText(columnIndex, newText);
				comp.dispose();
			}
		};
		
		editor.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
					comp.dispose(); // Cancel editing
				}
			}
		});
		
		editor.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (Util.isEnterKey(e.keyCode)) {
					e.doit = false;
					saver.run();
				}
			}
		});
		
		editor.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				comp.dispose();
			}
		});
		
		editor.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				saver.run();
			}
		});
		
		return comp;
	}
	
	@NotNull
	private Control createComboEditor(	@NotNull final ComboEditSupport<E, ?> editSupport,
										@NotNull final TableItem item,
										final int columnIndex) {
		final CCombo combo = new CCombo(table, SWT.READ_ONLY | SWT.FLAT);
		combo.setItems(editSupport.getStringChoices());
		combo.setText(item.getText(columnIndex));
		
		final Runnable saver = new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				E element = (E) item.getData();
				String newText = combo.getText();
				editSupport.setChoice(element, newText);
				item.setText(columnIndex, newText);
				combo.dispose();
			}
		};
		
		combo.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				saver.run();
			}
		});
		
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				/*
				 * This line will end the editing when the user changes the
				 * selection, either by clicking with the mouse or by pressing
				 * an arrow key. Ideally, the editing should only end in the
				 * former case, not in the latter. Unfortunately, the CCombo
				 * doesn't seem to allow us to distinguish between the two
				 * cases.
				 */
				saver.run();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				saver.run();
			}
		});
		
		Util.runAsyncExec(combo, new Runnable() {
			public void run() {
				combo.setListVisible(true);
			}
		});
		
		return combo;
	}
	
	public final void cancelEditing() {
		Control editor = tableEditor.getEditor();
		if (editor != null)
			editor.dispose();
	}
	
	@Nullable
	protected abstract ColumnEditSupport<E> getColumnEditSupport(int columnIndex);
	
}
