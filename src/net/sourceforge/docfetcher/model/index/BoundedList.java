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

package net.sourceforge.docfetcher.model.index;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
final class BoundedList<T> {
	
	private final int capacity;
	private LinkedList<T> list = new LinkedList<T>();
	
	public BoundedList(int capacity) {
		Util.checkThat(capacity >= 0);
		this.capacity = capacity;
	}

	public void add(@NotNull T element) {
		Util.checkNotNull(element);
		list.add(element);
		if (list.size() > capacity)
			list.removeFirst();
	}
	
	public void addAll(@NotNull Collection<T> elements) {
		Util.checkNotNull(elements);
		list.addAll(elements);
		while (list.size() > capacity)
			list.removeFirst();
	}
	
	@NotNull
	public List<T> removeAll() {
		List<T> ret = list;
		list = new LinkedList<T>();
		return ret;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}

}
