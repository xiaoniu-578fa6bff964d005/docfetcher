package net.sourceforge.docfetcher.util.collect;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import com.google.common.collect.ForwardingMap;

/**
 * A subclass of HashMap that deprecates the {@link Map#get(Object)} and
 * {@link Map#remove(Object)} methods in favor of the more restrictive
 * {@link SafeKeyMap#getValue(K)} and {@link SafeKeyMap#removeKey(K)} methods.
 * This class is useful in situations where it is very easy to accidentally use
 * the wrong map key, e.g. using a File object instead of a file path.
 */
public final class SafeKeyMap<K, V> extends ForwardingMap<K, V> {
	
	@NotNull
	public static <K, V> SafeKeyMap<K, V> createHashMap() {
		return new SafeKeyMap<K, V>(new HashMap<K, V>());
	}
	
	@NotNull
	public static <K, V> SafeKeyMap<K, V> createLinkedHashMap() {
		return new SafeKeyMap<K, V>(new LinkedHashMap<K, V>());
	}
	
	@NotNull
	public static <K, V> SafeKeyMap<K, V> createTreeMap() {
		return new SafeKeyMap<K, V>(new TreeMap<K, V>());
	}
	
	@NotNull
	public static <K, V> SafeKeyMap<K, V> create(@NotNull Map<K, V> delegate) {
		Util.checkNotNull(delegate);
		return new SafeKeyMap<K, V>(delegate);
	}
	
	private final Map<K, V> delegate;
	
	private SafeKeyMap(Map<K, V> delegate) {
		this.delegate = delegate;
	}
	
	protected Map<K, V> delegate() {
		return delegate;
	}

	@Deprecated
	public V get(Object key) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}
	
	@Deprecated
	public boolean containsKey(Object key) {
		throw new UnsupportedOperationException();
	}
	
	@Deprecated
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	public V getValue(K key) {
		return super.get(key);
	}

	public V removeKey(K key) {
		return super.remove(key);
	}
	
	public boolean containsKeySafe(K key) {
		return super.containsKey(key);
	}
	
	public boolean containsValueSafe(V value) {
		return super.containsValue(value);
	}
	
}