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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
public final class LazyList<T> extends AbstractList<T> {
	
	@Nullable private List<T> list;
	
	public T get(int index) {
		if (list == null)
			throw new IndexOutOfBoundsException();
		return list.get(index);
	}
	
	public int size() {
		if (list == null)
			return 0;
		return list.size();
	}
	
	public T set(int index, T element) {
		if (list == null)
			throw new IndexOutOfBoundsException();
		return list.set(index, element);
	}
	
	public void add(int index, T element) {
		getList().add(index, element);
	}
	
	public T remove(int index) {
		if (list == null)
			throw new IndexOutOfBoundsException();
		return list.remove(index);
	}
	
	@NotNull
	private List<T> getList() {
		if (list == null)
			list = new ArrayList<T>();
		return list;
	}
	
	@NotNull
	public Iterator<T> iterator() {
		if (list == null)
			return Iterators.emptyIterator();
		return list.iterator();
	}

}
