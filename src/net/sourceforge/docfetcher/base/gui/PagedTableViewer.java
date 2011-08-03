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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.Immutable;
import net.sourceforge.docfetcher.base.annotations.MutableCopy;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Supports pagination
 * 
 * @author Tran Nam Quang
 */
public abstract class PagedTableViewer<E> {
	
	public static abstract class ElementInfo<E> {
		protected boolean isChecked(E element) { return true; }
	}
	
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
		protected String getToolTipText() { return null; }
		
		@NotNull protected abstract String getLabel(E element);
		protected Image getImage(E element) { return null; }
		protected Color getForeground(E element) { return null; }
		protected Color getBackground(E element) { return null; }
	}
	
	@Nullable private Object rootElement;
	private final Table table;
	private final Map<E, TableItem> elementToItemMap = Maps.newHashMap();
	@Nullable private ElementInfo<E> elementInfo;
	private final List<Column<E>> columns = Lists.newArrayList();
	
	private int elementsPerPage = Integer.MAX_VALUE;
	private int pageIndex = 0;
	private final List<List<E>> emptyPages = Collections.singletonList(Collections.<E>emptyList());
	private List<List<E>> pages = emptyPages;
	
	public PagedTableViewer(@NotNull Composite parent, int style) {
		this(new Table(parent, style));
	}
	
	public PagedTableViewer(@NotNull final Table table) {
		if (Util.contains(table.getStyle(), SWT.VIRTUAL))
			throw new IllegalArgumentException("This class does not support virtual tables.");
		this.table = table;
		
		table.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			public void mouseDoubleClick(MouseEvent e) {
				TableItem item = table.getItem(new Point(e.x, e.y));
				if (item == null) return;
				onDoubleClick((E) item.getData());
			}
		});
	}
	
	@NotNull
	public final Table getControl() {
		return table;
	}
	
	public final void setElementInfo(@Nullable ElementInfo<E> elementInfo) {
		this.elementInfo = elementInfo;
	}
	
	public final void addColumn(@NotNull Column<E> column) {
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
	public final List<Column<E>> getColumns() {
		return Collections.unmodifiableList(columns);
	}
	
	// This does not reset the page index, but clamps it if necessary (explain why)
	public final void setRoot(@Nullable Object rootElement) {
		// Reset fields, excluding the pageIndex
		this.rootElement = rootElement;
		table.removeAll();
		elementToItemMap.clear();
		pages = emptyPages;
		if (columns.isEmpty()) return;
		
		/*
		 * Get elements and split them into pages
		 * 
		 * Lists.partition(...) returns an empty list (i.e. zero pages) if the
		 * input list is empty or if the page size is Integer.MAX_VALUE, which
		 * is not what we want.
		 */
		Collection<E> elements = getElements(rootElement);
		if (elements == null || elements.isEmpty())
			pages = emptyPages;
		else if (elementsPerPage == Integer.MAX_VALUE)
			pages = Collections.singletonList(filterAndSort(elements));
		else
			pages = Lists.partition(filterAndSort(elements), elementsPerPage);
		
		// Populate the table with the elements on the current page
		assert pageIndex >= 0;
		assert pages.size() >= 1;
		pageIndex = Util.clamp(pageIndex, 0, pages.size() - 1);
		List<E> pageElements = pages.get(pageIndex);
		onPageRefresh(pageElements);
		for (E element : pageElements) {
			TableItem item = new TableItem(table, SWT.NONE);
			update(element, item);
			elementToItemMap.put(element, item);
		}
	}
	
	@Nullable
	public final Object getRoot() {
		return rootElement;
	}
	
	// This does not reset the page index, but clamps it if necessary
	public final void refresh() {
		setRoot(rootElement);
	}
	
	private void update(E element, TableItem item) {
		int nCol = columns.size();
		if (nCol == 0) return;
		if (elementInfo != null)
			item.setChecked(elementInfo.isChecked(element));
		for (int iCol = 0; iCol < nCol; iCol++) {
			Column<E> column = columns.get(iCol);
			item.setText(iCol, column.getLabel(element));
			item.setImage(iCol, column.getImage(element));
			item.setForeground(iCol, column.getForeground(element));
			item.setBackground(iCol, column.getBackground(element));
		}
		item.setData(element);
	}
	
	@MutableCopy
	private List<E> filterAndSort(Collection<E> elements) {
		List<E> newElements = new ArrayList<E>(elements.size());
		for (E child : elements)
			if (filter(child))
				newElements.add(child);
		sort(newElements);
		return newElements;
	}
	
	@Nullable
	protected abstract Collection<E> getElements(@Nullable Object rootElement);
	
	protected void sort(@NotNull List<E> unsortedElements) {}
	
	// Returns true if the element should *not* be excluded
	protected boolean filter(@NotNull E element) { return true; }
	
	protected void onDoubleClick(@NotNull E element) { return; }
	
	// Called shortly before the current page is about to be populated with table items
	// The given elements are what's left of the original input
	// after sorting, filtering and paging have been applied
	protected void onPageRefresh(@NotNull List<E> currentPageElements) {}
	
	protected void onSelectionChanged(@NotNull E newSelection) {}
	
	@MutableCopy
	@NotNull
	@SuppressWarnings("unchecked")
	public final List<E> getChecked() {
		List<E> checkedElements = new ArrayList<E>();
		for (TableItem item : table.getItems())
			if (item.getChecked())
				checkedElements.add((E) item.getData());
		return checkedElements;
	}
	
	@MutableCopy
	@NotNull
	@SuppressWarnings("unchecked")
	public final List<E> getSelection() {
		TableItem[] selection = table.getSelection();
		List<E> selElements = new ArrayList<E>(selection.length);
		for (TableItem item : selection)
			selElements.add((E) item.getData());
		return selElements;
	}
	
	public final int getSelectionCount() {
		return table.getSelectionCount();
	}
	
	public final void update(@Nullable E element) {
		if (element == null) return;
		update(element, elementToItemMap.get(element));
	}
	
	public final void update() {
		for (Map.Entry<E, TableItem> entry : elementToItemMap.entrySet())
			update(entry.getKey(), entry.getValue());
	}
	
	// Switches to the appropriate page if necessary
	public final void showElement(@Nullable E element) {
		if (element == null) return;
		TableItem item = elementToItemMap.get(element);
		if (item != null) {
			table.showItem(item);
			table.setSelection(item);
			return;
		}
		// Switch to the page that contains the given element
		for (int i = 0; i < pages.size(); i++) {
			for (E candidate : pages.get(i)) {
				if (candidate != element) continue;
				pageIndex = i;
				refresh();
				TableItem item1 = elementToItemMap.get(element);
				table.showItem(item1);
				table.setSelection(item1);
				return;
			}
		}
	}
	
	@Immutable
	@NotNull
	public final List<E> getElements(int pageIndex) {
		Util.checkThat(pageIndex >= 0);
		Util.checkThat(pageIndex < pages.size());
		return Collections.unmodifiableList(pages.get(pageIndex));
	}
	
	public final boolean setFocus() {
		return table.setFocus();
	}

	// returned value is >= 1
	public final int getElementsPerPage() {
		assert elementsPerPage >= 1;
		return elementsPerPage;
	}
	
	// values <= 0 will be set to 1
	public final void setElementsPerPage(int elementsPerPage) {
		int oldElementsPerPage = this.elementsPerPage;
		this.elementsPerPage = Math.max(1, elementsPerPage);
		if (oldElementsPerPage != this.elementsPerPage) {
			pageIndex = 0;
			refresh();
		}
	}
	
	public final int getPageCount() {
		return pages.size();
	}
	
	// pageIndex is zero-based
	public final int getPageIndex() {
		assert pageIndex >= 0 && pageIndex < pages.size();
		return pageIndex;
	}
	
	// values outside [0, page.size() - 1] will be clamped
	// pageIndex is zero-based
	public final void setPage(int pageIndex) {
		int oldPageIndex = this.pageIndex;
		this.pageIndex = Util.clamp(pageIndex, 0, pages.size() - 1);
		if (oldPageIndex != this.pageIndex)
			refresh();
	}
	
	public final void previousPage() {
		setPage(--pageIndex);
	}
	
	public final void nextPage() {
		setPage(++pageIndex);
	}

}
