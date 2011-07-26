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

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.google.common.collect.Maps;

/**
 * A simplified, type-safe version of JFace's TreeViewer.
 * 
 * @author Tran Nam Quang
 */
public abstract class SimpleTreeViewer<E> {
	
	@Nullable private Iterable<E> rootElements;
	private final Tree tree;
	private final Map<E, TreeItem> elementToItemMap = Maps.newHashMap();
	private final ItemDisposeListener itemDisposeListener = new ItemDisposeListener();
	
	public SimpleTreeViewer(@NotNull Composite parent, int style) {
		this(new Tree(parent, style));
	}
	
	public SimpleTreeViewer(@NotNull final Tree tree) {
		this.tree = Util.checkNotNull(tree);
		tree.addTreeListener(new TreeListener() {
			public void treeExpanded(TreeEvent e) {
				loadNextButOneLevel((TreeItem) e.item);
			}
			public void treeCollapsed(TreeEvent e) {
				disposeNextButOneLevel((TreeItem) e.item);
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				TreeItem item = tree.getItem(new Point(e.x, e.y));
				if (item == null) return;
				if (item.getExpanded()) {
					item.setExpanded(false);
					disposeNextButOneLevel(item);
				}
				else {
					loadNextButOneLevel(item);
					item.setExpanded(true);
				}
			}
		});
		if (Util.contains(tree.getStyle(), SWT.CHECK)) {
			tree.addSelectionListener(new SelectionAdapter() {
				@SuppressWarnings("unchecked")
				public void widgetSelected(SelectionEvent e) {
					if (!Util.contains(e.detail, SWT.CHECK))
						return;
					TreeItem item = (TreeItem) e.item;
					E element = (E) item.getData();
					setChecked(element, item.getChecked());
				}
			});
		}
	}
	
	private void loadNextButOneLevel(TreeItem item) {
		E element = getElement(item);
		for (E child : getFilteredChildren(element))
			createChildItems(elementToItemMap.get(child), child);
	}
	
	private void disposeNextButOneLevel(TreeItem item) {
		for (TreeItem item2 : item.getItems())
			item2.removeAll();
	}

	@NotNull
	public final Tree getControl() {
		return tree;
	}
	
	@SuppressWarnings("unchecked")
	private E getElement(TreeItem item) {
		return (E) item.getData();
	}
	
	public final void setRoots(@Nullable Iterable<E> rootElements) {
		tree.removeAll();
		elementToItemMap.clear();
		this.rootElements = rootElements;
		if (rootElements == null) return;
		for (E rootElement : filterAndSort(rootElements))
			createRootItemWithChildren(rootElement, -1);
	}
	
	@Nullable
	public final Iterable<E> getRoots() {
		return rootElements;
	}
	
	private class ItemDisposeListener implements DisposeListener {
		public void widgetDisposed(DisposeEvent e) {
			TreeItem item = (TreeItem) e.widget;
			elementToItemMap.remove(getElement(item));
			
			/*
			 * This is not necessary because when an item is disposed, it will
			 * detach all of its listeners.
			 */
//			item.removeDisposeListener(this);
		}
	};
	
	private void createRootItemWithChildren(E element, int insertionIndex) {
		TreeItem item = insertionIndex < 0
			? new TreeItem(tree, SWT.NONE)
			: new TreeItem(tree, SWT.NONE, insertionIndex);
		update(element, item);
		item.addDisposeListener(itemDisposeListener);
		elementToItemMap.put(element, item);
		createChildItems(item, element);
	}
	
	private TreeItem createItem(TreeItem parentItem, E element) {
		TreeItem item = new TreeItem(parentItem, SWT.NONE);
		update(element, item);
		item.addDisposeListener(itemDisposeListener);
		elementToItemMap.put(element, item);
		return item;
	}

	private void createChildItems(TreeItem parentItem, E parentElement) {
		for (E child : getFilteredChildren(parentElement))
			createItem(parentItem, child);
	}
	
	private void update(E element, TreeItem item) {
		if (item == null) return;
		item.setImage(getImage(element));
		item.setText(getLabel(element));
		if (Util.contains(tree.getStyle(), SWT.CHECK))
			item.setChecked(isChecked(element));
		item.setForeground(getForeground(element));
		item.setBackground(getBackground(element));
		item.setData(element);
	}
	
	private Collection<E> filterAndSort(Iterable<E> elements) {
		List<E> newElements = new ArrayList<E> ();
		for (E child : elements)
			if (filter(child))
				newElements.add(child);
		sort(newElements);
		return newElements;
	}

	private Collection<E> getFilteredChildren(E parentElement) {
		Iterable<E> elements = getChildren(parentElement);
		if (elements == null)
			elements = Collections.emptyList();
		return filterAndSort(elements);
	}
	
	/**
	 * Returns the children of the given element, or null if there are no
	 * children.
	 */
	@Nullable
	protected abstract Iterable<E> getChildren(E element);
	
	@NotNull
	protected abstract String getLabel(@NotNull E element);
	
	protected boolean isChecked(@NotNull E element) {
		throw new UnsupportedOperationException();
	}
	
	protected void setChecked(@NotNull E element, boolean checked) {
		throw new UnsupportedOperationException();
	}
	
	@Nullable
	protected Image getImage(@NotNull E element) { return null; }
	
	@Nullable
	protected Color getForeground(@NotNull E element) { return null; }
	
	@Nullable
	protected Color getBackground(@NotNull E element) { return null; }
	
	protected void sort(@NotNull List<E> unsortedElements) {}
	
	protected boolean filter(@NotNull E element) { return true; }
	
	@NotNull
	public final List<E> getSelection() {
		TreeItem[] selectedItems = tree.getSelection();
		List<E> selection = new ArrayList<E>(selectedItems.length);
		for (TreeItem item : selectedItems)
			selection.add(getElement(item));
		return selection;
	}
	
	public final void setSelection(@Nullable E... elements) {
		if (elements == null) return;
		TreeItem[] items = new TreeItem[elements.length];
		for (int i = 0; i < elements.length; i++)
			items[i] = elementToItemMap.get(elements[i]);
		tree.setSelection(items);
	}
	
	public final int getSelectionCount() {
		return tree.getSelectionCount();
	}
	
	public final void update(@Nullable E element) {
		if (element == null) return;
		update(element, elementToItemMap.get(element));
	}
	
	public final void update() {
		for (Map.Entry<E, TreeItem> entry : elementToItemMap.entrySet())
			update(entry.getKey(), entry.getValue());
	}
	
	public final void refreshChildren(@Nullable E element) {
		if (element == null) return;
		TreeItem item = elementToItemMap.get(element);
		boolean expanded = item.getExpanded();
		Map<E, Boolean> expandedStates = getExpandedStates(element);
		item.removeAll();
		createChildItems(item, element);
		if (expanded)
			for (E child : getFilteredChildren(element))
				createChildItems(elementToItemMap.get(child), child);
		item.setExpanded(expanded);
		setExpandedStates(element, expandedStates);
	}
	
	public final void refresh() {
		setRoots(rootElements);
	}
	
	public final void addRootItem(@Nullable E element, int index) {
		if (element == null) return;
		Util.checkThat(index >= 0);
		createRootItemWithChildren(element, index);
	}
	
	public final void remove(@Nullable E element) {
		if (element == null) return;
		TreeItem item = elementToItemMap.get(element);
		item.removeAll();
		item.dispose();
	}
	
	public final void remove(@Nullable Iterable<E> elements) {
		if (elements == null) return;
		tree.setRedraw(false);
		for (E element : elements) {
			TreeItem item = elementToItemMap.get(element);
			
			/*
			 * No need to remove the element from the map, this will be done by
			 * the dispose listener that will be notified when the following
			 * methods are called.
			 */
			item.removeAll();
			item.dispose();
		}
		tree.setRedraw(true);
	}
	
	private Map<E, Boolean> getExpandedStates(E parent) {
		Map<E, Boolean> expandedStates = Maps.newHashMap();
		getExpandedStates(parent, expandedStates);
		return expandedStates;
	}
	
	private void getExpandedStates(E parent, Map<E, Boolean> expandedStates) {
		for (E child : getFilteredChildren(parent)) {
			TreeItem item = elementToItemMap.get(child);
			if (item == null) continue;
			expandedStates.put(child, item.getExpanded());
			getExpandedStates(child, expandedStates);
		}
	}
	
	private void setExpandedStates(E parent, Map<E, Boolean> expandedStates) {
		for (E child : getFilteredChildren(parent)) {
			Boolean expanded = expandedStates.get(child);
			if (expanded == null) continue;
			TreeItem item = elementToItemMap.get(child);
			if (item == null) continue;
			item.setExpanded(expanded);
			setExpandedStates(child, expandedStates);
		}
	}
	
	@NotNull
	public final List<E> getElements() {
		return new ArrayList<E> (elementToItemMap.keySet());
	}
	
	public final void expand(@Nullable E element) {
		if (element == null) return;
		TreeItem item = elementToItemMap.get(element);
		if (item.getExpanded()) return;
		for (E child : getFilteredChildren(element))
			createChildItems(elementToItemMap.get(child), child);
		item.setExpanded(true);
	}
	
	public final void collapse(@Nullable E element) {
		if (element == null) return;
		TreeItem item = elementToItemMap.get(element);
		if (! item.getExpanded()) return;
		for (E child : getFilteredChildren(element))
			elementToItemMap.get(child).removeAll();
		item.setExpanded(false);
	}
	
	/**
	 * Returns the element at the given point in the receiver or null if no such
	 * element exists. The point is in the coordinate system of the receiver.
	 */
	@Nullable
	public final E getElement(@Nullable Point point) {
		TreeItem item = tree.getItem(point);
		return getElement(item);
	}
	
	public final void showElement(@Nullable E element) {
		if (element == null) return;
		TreeItem item = elementToItemMap.get(element);
		tree.showItem(item);
		tree.setSelection(item);
	}

	public final boolean setFocus() {
		return tree.setFocus();
	}
	
}
