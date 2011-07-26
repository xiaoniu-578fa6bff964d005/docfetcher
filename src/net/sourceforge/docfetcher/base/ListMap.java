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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.docfetcher.base.ListMap.Entry;
import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class ListMap<K, V> implements Iterable<Entry<K, V>> {
	
	public static final class Entry<K, V> {
		private final K key;
		private final V value;
		
		private Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@NotNull
		public K getKey() {
			return key;
		}
		
		@NotNull
		public V getValue() {
			return value;
		}
	}
	
	private final List<Entry<K, V>> list;
	
	public ListMap() {
		list = new ArrayList<Entry<K, V>>();
	}
	
	public ListMap(int size) {
		list = new ArrayList<Entry<K, V>>(size);
	}
	
	public void add(@NotNull K key, @NotNull V value) {
		Util.checkNotNull(key, value);
		list.add(new Entry<K, V>(key, value));
	}
	
	public int size() {
		return list.size();
	}
	
	@NotNull
	public Iterator<Entry<K, V>> iterator() {
		return list.iterator();
	}
	
	public void sort(@NotNull Comparator<Entry<K, V>> comparator) {
		Collections.sort(list, comparator);
	}

}
