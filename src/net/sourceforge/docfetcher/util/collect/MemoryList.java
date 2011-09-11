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

package net.sourceforge.docfetcher.util.collect;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class MemoryList<E> implements Collection<E> {
	
	/*
	 * The internal list is sorted from most to least recently accessed.
	 */
	private final LinkedList<E> list = new LinkedList<E>();
	private final int capacity;

	public MemoryList(int capacity) {
		Util.checkThat(capacity >= 0);
		this.capacity = capacity;
	}

	/**
	 * Adds the given element to the receiver, with the following constraints:
	 * <ul>
	 * <li>New elements are placed at the beginning of the list.</li>
	 * <li>If one attempts to add an element that is already in the list,
	 * instead of duplication the existing element is moved to the beginning of
	 * the list.</li>
	 * <li>The list has a capacity limit. When that limit is reached, adding a
	 * new elements leads to removal of the oldest element, which is at the end
	 * of the list.</li>
	 * </ul>
	 */
	public boolean add(E e) {
		if (!list.isEmpty() && list.getFirst().equals(e))
			return false;
		list.remove(e);
		list.addFirst(e);
		if (list.size() > capacity)
			list.removeLast();
		return true;
	}

	/**
	 * Has the same effect as iterating over the given collection and calling
	 * {@link #add(Object)} for each element.
	 * 
	 * @see #add(Object)
	 */
	public boolean addAll(Collection<? extends E> c) {
		boolean changed = false;
		for (E e : c)
			changed |= add(e);
		return changed;
	}
	
	/**
	 * Returns the most recent element in the list. Throws a
	 * {@link NoSuchElementException} if the list is empty.
	 */
	@NotNull
	public E getMostRecent() {
		return list.getFirst();
	}

	public int size() {
		return list.size();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public Iterator<E> iterator() {
		return list.iterator();
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	public boolean remove(Object o) {
		return list.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	public void clear() {
		list.clear();
	}

}
