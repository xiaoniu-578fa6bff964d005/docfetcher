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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
public abstract class VirtualTableViewer<E> {
	
	public static abstract class Column<E> {
		private String label;
		private final int orientation;
		private final Event<String> evtLabelChanged = new Event<String> ();
		private int lastSortDirection = 1;
		
		public Column(@NotNull String label) {
			this(label, SWT.LEFT);
		}
		public Column(@NotNull String label, int orientation) {
			this.label = Util.checkNotNull(label);
			this.orientation = orientation;
		}
		@NotNull
		public final String getLabel() {
			return label;
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
		protected int compare(@NotNull E e1, @NotNull E e2) { return 0; }
	}
	
	private final Table table;
	private final List<Column<E>> columns = new ArrayList<Column<E>>();
	private List<E> elements;
	private boolean sortingEnabled = false;
	@Nullable private Column<E> lastSortColumn = null;
	
	public VirtualTableViewer(@NotNull Composite parent, int style) {
		table = new Table(parent, style | SWT.VIRTUAL);
		table.setHeaderVisible(true);
		
		table.addListener(SWT.SetData, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				// Bug #3523251: event.index can be -1 sometimes, looks like a
				// bug in SWT
				if (event.index < 0)
					return;
				Util.checkThat(!columns.isEmpty());
				TableItem item = (TableItem) event.item;
				E element = elements.get(event.index);
				for (int iCol = 0; iCol < columns.size(); iCol++) {
				    Column<E> column = columns.get(iCol);
				    item.setText(iCol, column.getLabel(element));
				    item.setImage(iCol, column.getImage(element));
				    item.setForeground(iCol, column.getForeground(element));
				    item.setBackground(iCol, column.getBackground(element));
				}
				item.setData(element);
			}
		});
	}
	
	@NotNull
	public final Table getControl() {
		return table;
	}
	
	public final void addColumn(@NotNull final Column<E> column) {
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
		
		tableColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sortByColumn(column);
			}
		});
	}
	
	public final void sortByColumn(@NotNull final Column<E> column) {
		if (elements == null || !sortingEnabled)
			return;
		final int direction = lastSortColumn != column
			? 1
			: column.lastSortDirection * -1;
		Collections.sort(elements, new Comparator<E>() {
			public int compare(E e1, E e2) {
				return column.compare(e1, e2) * direction;
			};
		});
		table.clearAll();
		lastSortColumn = column;
		column.lastSortDirection = direction;
	}
	
	public final void sortByColumn(@NotNull final Column<E> column, boolean up) {
		if (elements == null || !sortingEnabled)
			return;
		final int direction = up ? 1 : -1;
		Collections.sort(elements, new Comparator<E>() {
			public int compare(E e1, E e2) {
				return column.compare(e1, e2) * direction;
			};
		});
		table.clearAll();
		lastSortColumn = column;
		column.lastSortDirection = direction;
	}
	
	@Immutable
	@NotNull
	public final List<Column<E>> getColumns() {
		return Collections.unmodifiableList(columns);
	}
	
	@Immutable
	@NotNull
	public final List<Column<E>> getColumnsVisualOrder() {
		List<Column<E>> visualColumns = new ArrayList<Column<E>>(columns.size());
		for (int index : table.getColumnOrder())
			visualColumns.add(columns.get(index));
		return Collections.unmodifiableList(visualColumns);
	}
	
	// does not take sorting into account
	public final void setRoot(@NotNull Object rootElement) {
		Util.checkNotNull(rootElement);
		Util.checkThat(!columns.isEmpty());
		elements = Util.checkNotNull(getElements(rootElement));
		table.setItemCount(elements.size()); // Must be called *before* calling clearAll()
		table.clearAll();
		lastSortColumn = null;
	}
	
	@MutableCopy
	@NotNull
	@SuppressWarnings("unchecked")
	public final List<E> getSelection() {
		TableItem[] selection = table.getSelection();
		List<E> selElements = new ArrayList<E>(selection.length);
		for (TableItem item : selection) {
			E element = (E) item.getData();
			/*
			 * Bug #3532164: Not sure why, but the item data can be null
			 * sometimes. Possibly an SWT bug.
			 */
			if (element != null)
				selElements.add(element);
		}
		return selElements;
	}
	
	public final void scrollToTop() {
		ScrollBar verticalBar = table.getVerticalBar();
		if (verticalBar != null)
			verticalBar.setSelection(0);
	}
	
	public final void scrollToBottom() {
		TableItem lastItem = table.getItem(table.getItemCount() - 1);
		table.showItem(lastItem);
	}
	
	// when sorting is enabled, override the compare method on all sortable columns
	public void setSortingEnabled(boolean sortingEnabled) {
		this.sortingEnabled = sortingEnabled;
		if (!sortingEnabled)
			lastSortColumn = null;
	}
	
	@NotNull
	protected abstract List<E> getElements(@NotNull Object rootElement);

}
