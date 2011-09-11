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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
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
	
	private final Table table;
	private final List<Column<E>> columns = new ArrayList<Column<E>>();
	private List<E> elements;
	
	public VirtualTableViewer(@NotNull Composite parent, int style) {
		table = new Table(parent, style | SWT.VIRTUAL);
		
		table.addListener(SWT.SetData, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				Util.checkThat(!columns.isEmpty());
				TableItem item = (TableItem) event.item;
				E element = elements.get(event.index);
				int nCol = columns.size();
				for (int iCol = 0; iCol < nCol; iCol++) {
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
	
	public final void setRoot(@NotNull Object rootElement) {
		Util.checkNotNull(rootElement);
		Util.checkThat(!columns.isEmpty());
		elements = Util.checkNotNull(getElements(rootElement));
		table.clearAll();
		table.setItemCount(elements.size());
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
	
	public final void scrollToTop() {
		ScrollBar verticalBar = table.getVerticalBar();
		if (verticalBar != null)
			verticalBar.setSelection(0);
	}
	
	@NotNull
	protected abstract List<E> getElements(@NotNull Object rootElement);

}
