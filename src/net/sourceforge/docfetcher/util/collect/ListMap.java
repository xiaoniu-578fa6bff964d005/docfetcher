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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.collect.ListMap.Entry;

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
	
	@NotNull
	public static <K, V> ListMap<K, V> create() {
		return new ListMap<K, V>();
	}
	
	@NotNull
	public static <K, V> ListMap<K, V> create(int size) {
		return new ListMap<K, V>(size);
	}
	
	private final List<Entry<K, V>> list;
	
	private ListMap() {
		list = new ArrayList<Entry<K, V>>();
	}
	
	private ListMap(int size) {
		list = new ArrayList<Entry<K, V>>(size);
	}
	
	// Returns itself for method chaining
	@NotNull
	public ListMap<K, V> add(@NotNull K key, @NotNull V value) {
		Util.checkNotNull(key, value);
		list.add(new Entry<K, V>(key, value));
		return this;
	}
	
	/**
	 * Returns the value of the <b>first</b> occurrence of the given key, or
	 * null if there is no mapping for the given key.
	 */
	@Nullable
	public V getValue(@NotNull K key) {
		Util.checkNotNull(key);
		for (Entry<K, V> entry : list)
			if (entry.key.equals(key))
				return entry.value;
		return null;
	}
	
	/**
	 * Returns the key of the <b>first</b> occurrence of the given value, or
	 * null if there is no mapping for the given value.
	 */
	@Nullable
	public K getKey(@NotNull V value) {
		Util.checkNotNull(value);
		for (Entry<K, V> entry : list)
			if (entry.value.equals(value))
				return entry.key;
		return null;
	}
	
	public int size() {
		return list.size();
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public boolean containsKey(@NotNull K key) {
		return getValue(key) != null;
	}
	
	public boolean containsValue(@NotNull V value) {
		return getKey(value) != null;
	}
	
	public boolean removeKey(@NotNull K key) {
		Util.checkNotNull(key);
		Iterator<Entry<K, V>> it = list.iterator();
		boolean removed = false;
		while (it.hasNext()) {
			Entry<K, V> entry = it.next();
			if (entry.key.equals(key)) {
				it.remove();
				removed = true;
			}
		}
		return removed;
	}
	
	public boolean removeValue(@NotNull V value) {
		Util.checkNotNull(value);
		Iterator<Entry<K, V>> it = list.iterator();
		boolean removed = false;
		while (it.hasNext()) {
			Entry<K, V> entry = it.next();
			if (entry.value.equals(value)) {
				it.remove();
				removed = true;
			}
		}
		return removed;
	}
	
	@NotNull
	public Entry<K, V> getEntry(int index) {
		return list.get(index);
	}
	
	@NotNull
	public Iterator<Entry<K, V>> iterator() {
		return list.iterator();
	}
	
	@MutableCopy
	@NotNull
	public List<K> getKeys() {
		List<K> keys = new ArrayList<K>(list.size());
		for (Entry<K, V> entry : list)
			keys.add(entry.key);
		return keys;
	}
	
	@MutableCopy
	@NotNull
	public List<V> getValues() {
		List<V> values = new ArrayList<V>(list.size());
		for (Entry<K, V> entry : list)
			values.add(entry.value);
		return values;
	}
	
	public void sort(@NotNull Comparator<Entry<K, V>> comparator) {
		Collections.sort(list, comparator);
	}

}
