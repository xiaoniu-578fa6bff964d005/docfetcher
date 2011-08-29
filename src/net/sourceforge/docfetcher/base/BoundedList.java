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

package net.sourceforge.docfetcher.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class BoundedList<T> implements Iterable<T> {
	
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

	public void add(@NotNull T element) {
		Util.checkNotNull(element);
		if (removeEldest) {
			list.add(element);
			if (list.size() > capacity)
				list.removeFirst();
		}
		else {
			if (list.size() < capacity)
				list.add(element);
		}
		virtualSize++;
	}
	
	public void addAll(@NotNull Collection<T> elements) {
		Util.checkNotNull(elements);
		if (elements.isEmpty())
			return;
		if (removeEldest) {
			list.addAll(elements);
			while (list.size() > capacity)
				list.removeFirst();
		}
		else {
			int diff = capacity - list.size();
			assert diff >= 0;
			if (diff > 0) {
				Iterator<T> it = elements.iterator();
				for (int i = 0; i < diff && it.hasNext(); i++)
					list.add(it.next());
			}
		}
		virtualSize += elements.size();
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
	
	public int size() {
		return list.size();
	}
	
	@NotNull
	public List<T> removeAll() {
		List<T> ret = list;
		list = new LinkedList<T>();
		virtualSize = 0;
		return ret;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public boolean containsEq(@NotNull T element) {
		Util.checkNotNull(element);
		for (T candidate : list)
			if (candidate.equals(element))
				return true;
		return false;
	}
	
	public boolean containsId(@NotNull T element) {
		Util.checkNotNull(element);
		for (T candidate : list)
			if (candidate == element)
				return true;
		return false;
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

}
