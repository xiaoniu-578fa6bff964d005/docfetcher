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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.collect.ListMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
public final class SimpleTableViewer<E> {
	
	public static abstract class Column<E> {
		private String label;
		private final int orientation;
		private final Event<String> evtLabelChanged = new Event<String> ();
		
		public Column(@NotNull String label) {
			this(label, SWT.LEFT);
		}
		public Column(@NotNull String label, int orientation) {
			this.label = Util.checkNotNull(label);
			this.orientation = orientation;
		}
		public final void setLabel(@NotNull String label) {
			Util.checkNotNull(label);
			if (this.label.equals(label)) return;
			this.label = label;
			evtLabelChanged.fire(label);
		}
		
		@NotNull protected abstract String getLabel(E element);
		@Nullable protected String getToolTipText() { return null; }
		@Nullable protected Image getImage(E element) { return null; }
		@Nullable protected Color getForeground(E element) { return null; }
		@Nullable protected Color getBackground(E element) { return null; }
		@Nullable protected ColumnEditSupport<E> getEditSupport() { return null; }
	}
	
	private final Table table;
	private final List<Column<E>> columns = new ArrayList<Column<E>>();
	private final ListMap<E, TableItem> elementToItemMap = ListMap.create(); // elements are in same order as table items
	private final ItemDisposeListener itemDisposeListener = new ItemDisposeListener();
	@Nullable private TableEditSupport<E> editSupport;
	
	public SimpleTableViewer(@NotNull Composite parent, int style) {
		Util.checkThat(!Util.contains(style, SWT.VIRTUAL));
		table = new Table(parent, style);
		table.setHeaderVisible(true);
	}
	
	@NotNull
	public Table getControl() {
		return table;
	}
	
	public void addColumn(@NotNull Column<E> column) {
		Util.checkNotNull(column);
		columns.add(column);
		
		final TableColumn tableColumn = new TableColumn(table, column.orientation);
		tableColumn.setText(column.label);
		tableColumn.setToolTipText(column.getToolTipText());
		
		column.evtLabelChanged.add(new Event.Listener<String>() {
			public void update(String eventData) {
				tableColumn.setText(eventData);
			}
		});
	}
	
	@Immutable
	@NotNull
	public List<Column<E>> getColumns() {
		return Collections.unmodifiableList(columns);
	}
	
	public void add(@NotNull E element) {
		if (elementToItemMap.containsKey(element))
			return;
		TableItem item = new TableItem(table, SWT.NONE);
		updateItem(element, item);
		item.addDisposeListener(itemDisposeListener);
		elementToItemMap.add(element, item);
	}
	
	public void remove(@NotNull E element) {
		Util.checkNotNull(element);
		
		// This will automatically remove the element from the map
		elementToItemMap.getValue(element).dispose();
	}
	
	private void updateItem(@NotNull E element, @NotNull TableItem item) {
		for (int iCol = 0; iCol < columns.size(); iCol++) {
			Column<E> column = columns.get(iCol);
			item.setText(iCol, column.getLabel(element));
			item.setImage(iCol, column.getImage(element));
			item.setForeground(iCol, column.getForeground(element));
			item.setBackground(iCol, column.getBackground(element));
		}
		item.setData(element);
	}
	
	@SuppressWarnings("unchecked")
	public void move(@NotNull E element, boolean upNotDown) {
		Util.checkNotNull(element);
		
		TableItem item = elementToItemMap.getValue(element);
		int index = table.indexOf(item);
		assert index >= 0;
		
		if (index == 0 && upNotDown)
			return;
		if (index == table.getItemCount() - 1 && !upNotDown)
			return;
		
		if (editSupport != null)
			editSupport.cancelEditing();
		
		int adjacentIndex = index + (upNotDown ? -1 : 1);
		TableItem adjacentItem = table.getItem(adjacentIndex);
		E adjacentElement = (E) adjacentItem.getData();
		
		// Must preserve correct order of keys in element-to-item map
		updateItem(element, adjacentItem);
		updateItem(adjacentElement, item);
		elementToItemMap.replaceKey(element, adjacentItem);
		elementToItemMap.replaceKey(adjacentElement, item);
		
		List<TableItem> selection = Arrays.asList(table.getSelection());
		if (!selection.isEmpty()) {
			boolean itemSelected = selection.contains(item);
			boolean adjacentItemSelected = selection.contains(adjacentItem);
			
			if (itemSelected)
				table.select(adjacentIndex);
			else
				table.deselect(adjacentIndex);
			
			if (adjacentItemSelected)
				table.select(index);
			else
				table.deselect(index);
		}
	}
	
	public int getItemCount() {
		return elementToItemMap.size();
	}
	
	@NotNull
	public void showElement(int index) {
		Util.checkThat(!elementToItemMap.isEmpty());
		Util.checkThat(index >= 0 && index < elementToItemMap.size());
		TableItem item = elementToItemMap.getEntry(index).getValue();
		table.showItem(item);
		table.setSelection(index);
	}
	
	@MutableCopy
	@NotNull
	@SuppressWarnings("unchecked")
	public List<E> getSelection() {
		TableItem[] selection = table.getSelection();
		List<E> selElements = new ArrayList<E>(selection.length);
		for (TableItem item : selection)
			selElements.add((E) item.getData());
		return selElements;
	}
	
	@MutableCopy
	@NotNull
	public List<E> getElements() {
		return elementToItemMap.getKeys();
	}
	
	// Implementors should override Column.getEditSupport() for each editable column
	public void enableEditSupport() {
		if (editSupport != null)
			return;
		editSupport = new TableEditSupport<E>(table) {
			protected ColumnEditSupport<E> getColumnEditSupport(int columnIndex) {
				return columns.get(columnIndex).getEditSupport();
			}
		};
	}
	
	private final class ItemDisposeListener implements DisposeListener {
		public void widgetDisposed(DisposeEvent e) {
			TableItem item = (TableItem) e.widget;
			elementToItemMap.removeValue(item);
		}
	}

}
