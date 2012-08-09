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

package net.sourceforge.docfetcher.model;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.collect.SafeKeyMap;

import com.google.common.annotations.VisibleForTesting;

/**
 * Cold items are eligible for deletion, depending on the cold cache size. Cold
 * cache is a LRU cache. Hot items are not deleted until client calls dispose
 * method. Retrieving cold items moves them to the hot cache when get(key) is
 * called; if getCold(key) is called, cold items are left cold.
 * 
 * The primary purpose of this file cache is not to improve performance (the
 * speedup is probably tiny in most cases), but to keep unpacked files around
 * for a while, so as to avoid the nasty surprise of deleting files which are
 * currently open in an external application.
 * 
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class HotColdFileCache {
	
	public static final class PermanentFileResource implements FileResource {
		private final File file;
		
		public PermanentFileResource(@NotNull File file) {
			this.file = Util.checkNotNull(file);
		}
		@NotNull
		public File getFile() {
			return file;
		}
		public void dispose() {
		}
	}
	
	private static final class TemporaryFileResource implements FileResource {
		private final File file;
		private final HotColdFileCache cache;
		private final String key;
		private final File deletable;
		private volatile int useCount;
		
		// Creates deletable resource
		public TemporaryFileResource(	@NotNull File file,
										@NotNull HotColdFileCache cache,
										@NotNull String key,
										@NotNull File deletable,
										int useCount) {
			Util.checkNotNull(file, cache, key, deletable);
			Util.checkThat(!cache.coldCache.containsKeySafe(key));
			Util.checkThat(!cache.hotCache.containsKeySafe(key));
			this.file = file;
			this.cache = cache;
			this.key = key;
			this.deletable = deletable;
			assert useCount >= 0;
			this.useCount = useCount;
		}
		@NotNull
		public File getFile() {
			return file;
		}
		public void dispose() {
			cache.coolDown(key);
		}
	}
	
	// Helper proxy class to ensure that any resource returned from the cache
	// can only be disposed once.
	// Without this, clients would not be allowed to call dispose multiple times
	// without interfering with other clients.
	private static final class DisposeOnceProxyResource implements FileResource {
		private final FileResource innerResource;
		private boolean disposed = false;

		public DisposeOnceProxyResource(@NotNull FileResource innerResource) {
			this.innerResource = Util.checkNotNull(innerResource);
		}
		@NotNull
		public File getFile() {
			return innerResource.getFile();
		}
		public synchronized void dispose() {
			if (disposed) return;
			innerResource.dispose();
			disposed = true;
		}
	}
	
	private final SafeKeyMap<String, TemporaryFileResource> hotCache = SafeKeyMap.createHashMap();
	private final SafeKeyMap<String, TemporaryFileResource> coldCache;
	
	public HotColdFileCache(int coldCacheSize) {
		Util.checkThat(coldCacheSize >= 1);
		coldCache = SafeKeyMap.create(new ColdCache(coldCacheSize));
	}
	
	@VisibleForTesting
	public synchronized int getActualCacheSize() {
		return coldCache.size() + hotCache.size();
	}
	
	// Returns resource from either hot or cold cache.
	// If item found in cold cache, item is moved to hot cache.
	// If item found in hot cache, its use count is incremented.
	@Nullable
	public synchronized FileResource get(@NotNull Path key) {
		String absKey = key.getCanonicalPath();
		TemporaryFileResource hotItem = hotCache.getValue(absKey);
		TemporaryFileResource coldItem = coldCache.removeKey(absKey); // Remove from cold cache
		
		// The same resource must not be present in both caches
		if (hotItem != null && coldItem != null)
			throw new IllegalStateException();
		
		if (hotItem != null) {
			assert hotItem.useCount >= 1;
			hotItem.useCount++;
			return new DisposeOnceProxyResource(hotItem);
		}
		else if (coldItem != null) {
			assert coldItem.useCount == 0;
			coldItem.useCount = 1;
			hotCache.put(absKey, coldItem);
			return new DisposeOnceProxyResource(coldItem);
		}
		else {
			return null;
		}
	}
	
	@NotNull
	public synchronized FileResource putIfAbsent(	@NotNull Path key,
													@NotNull File deletableFile) {
		return putIfAbsent(key, deletableFile, deletableFile);
	}
	
	// If the cache already contains the given key,
	// returns the file resource associated with that key and deletes the given deletable
	@NotNull
	public synchronized FileResource putIfAbsent(	@NotNull Path key,
													@NotNull File file,
													@NotNull File deletable) {
		Util.checkNotNull(key, file, deletable);
		FileResource fileResource = get(key);
		if (fileResource != null) {
			try {
				Util.deleteRecursively(deletable);
			}
			catch (IOException e) {
				Util.printErr(e);
			}
			return new DisposeOnceProxyResource(fileResource);
		}
		String absKey = key.getCanonicalPath();
		TemporaryFileResource newFileResource = new TemporaryFileResource(
			file, this, absKey, deletable, 1);
		hotCache.put(absKey, newFileResource);
		return new DisposeOnceProxyResource(newFileResource);
	}
	
	private synchronized void coolDown(@NotNull String absKey) {
		TemporaryFileResource coldItem = coldCache.getValue(absKey);
		TemporaryFileResource hotItem = hotCache.getValue(absKey);
		
		// The same resource must not be present in both caches
		if (hotItem != null && coldItem != null)
			throw new IllegalStateException();
		
		// Fail if item not in hot cache
		if (hotItem == null)
			throw new UnsupportedOperationException();
		
		// Decrease use count; move item to cold cache when use count hits zero
		assert hotItem.useCount >= 1;
		hotItem.useCount = Math.max(0, hotItem.useCount - 1);
		if (hotItem.useCount == 0) {
			hotCache.removeKey(absKey);
			coldCache.put(absKey, hotItem); // Move to front
		}
	}
	
	// A simple LRU cache
	private static final class ColdCache extends LinkedHashMap<String, TemporaryFileResource> {
		private static final long serialVersionUID = 1L;
		
		@VisibleForTesting final int capacity;
		
		public ColdCache(int capacity) {
			super(capacity + 1, 0.75f, true);
			this.capacity = capacity;
		}
		protected boolean removeEldestEntry(Map.Entry<String, TemporaryFileResource> eldest) {
			if (size() <= capacity)
				return false;
			TemporaryFileResource fileResource = eldest.getValue();
			assert fileResource.deletable != null;
			try {
				// This will delete both files and directories
				Util.deleteRecursively(fileResource.deletable);
			}
			catch (IOException e) {
				Util.printErr(e);
			}
			return true;
		}
	}

}
