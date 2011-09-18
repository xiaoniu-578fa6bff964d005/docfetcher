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

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import com.google.common.collect.ForwardingCollection;

/**
 * @author Tran Nam Quang
 */
public final class BoundedList<T> extends ForwardingCollection<T> {
	
	private final int capacity;
	private final boolean removeEldest;
	private int virtualSize = 0;
	private LinkedList<T> list = new LinkedList<T>();
	
	public BoundedList(int capacity) {
		this(capacity, true);
	}
	
	public BoundedList(int capacity, boolean removeEldest) {
		Util.checkThat(capacity >= 0);
		this.capacity = capacity;
		this.removeEldest = removeEldest;
	}
	
	protected Collection<T> delegate() {
		return list;
	}

	public boolean add(@NotNull T element) {
		Util.checkNotNull(element);
		boolean added = capacity > 0;
		if (removeEldest) {
			list.add(element);
			if (list.size() > capacity)
				list.removeFirst();
		}
		else {
			if (list.size() < capacity)
				list.add(element);
			else
				added = false;
		}
		virtualSize++;
		return added;
	}
	
	public boolean addAll(@NotNull Collection<? extends T> elements) {
		Util.checkNotNull(elements);
		if (elements.isEmpty())
			return false;
		boolean added = capacity > 0;
		if (removeEldest) {
			list.addAll(elements);
			while (list.size() > capacity)
				list.removeFirst();
		}
		else {
			int diff = capacity - list.size();
			assert diff >= 0;
			if (diff > 0) {
				Iterator<? extends T> it = elements.iterator();
				for (int i = 0; i < diff && it.hasNext(); i++)
					list.add(it.next());
			}
			else {
				added = false;
			}
		}
		virtualSize += elements.size();
		return added;
	}
	
	@NotNull
	public T getLast() {
		return list.getLast();
	}
	
	public int getVirtualSize() {
		return virtualSize;
	}
	
	public int getCapacity() {
		return capacity;
	}
	
	@NotNull
	public Iterator<T> iterator() {
		final Iterator<T> innerIt = list.iterator();
		return new Iterator<T>() {
			public boolean hasNext() {
				return innerIt.hasNext();
			}
			public T next() {
				return innerIt.next();
			}
			public void remove() {
				innerIt.remove();
				virtualSize--;
				assert virtualSize >= 0;
			}
		};
	}

	public boolean remove(Object o) {
		boolean removed = list.remove(o);
		if (removed)
			virtualSize--;
		assert virtualSize >= 0;
		return removed;
	}

	public boolean removeAll(Collection<?> c) {
		Iterator<T> it = list.iterator();
		boolean changed = false;
		while (it.hasNext()) {
			T next = it.next();
			if (c.contains(next)) {
				it.remove();
				virtualSize--;
				changed = true;
			}
		}
		assert virtualSize >= 0;
		return changed;
	}

	public boolean retainAll(Collection<?> c) {
		Iterator<T> it = list.iterator();
		boolean changed = false;
		while (it.hasNext()) {
			T next = it.next();
			if (!c.contains(next)) {
				it.remove();
				virtualSize--;
				changed = true;
			}
		}
		assert virtualSize >= 0;
		return changed;
	}

	public void clear() {
		list.clear();
		virtualSize = 0;
	}

}
