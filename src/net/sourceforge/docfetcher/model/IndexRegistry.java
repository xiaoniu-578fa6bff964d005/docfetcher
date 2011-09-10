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

package net.sourceforge.docfetcher.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.sourceforge.docfetcher.base.BlockingWrapper;
import net.sourceforge.docfetcher.base.DelayedExecutor;
import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.CallOnce;
import net.sourceforge.docfetcher.base.annotations.ImmutableCopy;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.ThreadSafe;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.model.index.IndexingQueue;
import net.sourceforge.docfetcher.model.index.file.FileFactory;
import net.sourceforge.docfetcher.model.index.outlook.OutlookMailFactory;
import net.sourceforge.docfetcher.model.search.Searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.util.Version;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
@ThreadSafe
public final class IndexRegistry {

	public interface ExistingIndexesHandler {
		public void handleExistingIndexes(@NotNull List<LuceneIndex> indexes);
	}

	/*
	 * TODO code convention: Don't access Version elsewhere, don't instantiate
	 * Analyzer+ elsewhere, don't call setMaxClauseCount elsehwere.
	 */
	@VisibleForPackageGroup
	public static final Version LUCENE_VERSION = Version.LUCENE_30;

	@VisibleForPackageGroup
	public static final Analyzer analyzer = new StandardAnalyzer(
		LUCENE_VERSION, Collections.EMPTY_SET);

	private static final String SER_FILENAME = "tree-index.ser";

	/*
	 * This setting prevents errors that would otherwise occur if the user
	 * enters generic search terms like "*?".
	 */
	static {
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
	}

	// These events must always be fired under lock!
	// Avoid firing while holding only the read-lock, since it cannot be upgraded to a write-lock
	private final Event<LuceneIndex> evtAdded = new Event<LuceneIndex>();
	private final Event<List<LuceneIndex>> evtRemoved = new Event<List<LuceneIndex>>();

	/**
	 * A map for storing the indexes, along with the last-modified values of the
	 * indexes' ser files. A last-modified value may be null, which indicates
	 * that the corresponding index hasn't been saved yet.
	 */
	private final Map<LuceneIndex, Long> indexes = Maps.newTreeMap(IndexComparator.instance); // guarded by read-write lock
	
	/*
	 * This read-write lock is used for the index registry, the indexing queue,
	 * the searcher and the folder watcher. With the exception of the searcher,
	 * a read-write lock might not be the best choice for these classes in terms
	 * of efficiency. However, by using the same lock for all classes, we can
	 * avoid potential lock-ordering deadlocks.
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();
	
	private final File indexParentDir;
	private final IndexingQueue queue;
	private final HotColdFileCache unpackCache;
	private final FileFactory fileFactory;
	private final OutlookMailFactory outlookMailFactory;
	private final BlockingWrapper<Searcher> searcher = new BlockingWrapper<Searcher>();

	public IndexRegistry(	@NotNull File indexParentDir,
							int cacheSize,
							int reporterCapacity) {
		Util.checkNotNull(indexParentDir);
		this.indexParentDir = indexParentDir;
		this.unpackCache = new HotColdFileCache(cacheSize);
		this.fileFactory = new FileFactory(unpackCache);
		this.outlookMailFactory = new OutlookMailFactory(unpackCache);
		
		/*
		 * Giving out a reference to the IndexRegistry before it is fully
		 * constructed might be a little dangerous :-/
		 */
		this.queue = new IndexingQueue(this, reporterCapacity);
	}
	
	@NotNull
	@ThreadSafe
	public File getIndexParentDir() {
		return indexParentDir;
	}
	
	@NotNull
	@ThreadSafe
	public IndexingQueue getQueue() {
		return queue;
	}
	
	// Will block until the searcher is available (i.e. after load(...) has finished)
	// May return null if the calling thread was interrupted
	@Nullable
	@ThreadSafe
	public Searcher getSearcher() {
		/*
		 * This method must not be synchronized, otherwise we'll get a deadlock
		 * when this method is called while the load method is running.
		 */
		return searcher.get();
	}
	
	// Should not be used by clients
	@NotNull
	@ThreadSafe
	public Lock getReadLock() {
		return readLock;
	}
	
	// Should not be used by clients
	@NotNull
	@ThreadSafe
	public Lock getWriteLock() {
		return writeLock;
	}

	@ThreadSafe
	@VisibleForPackageGroup
	public void addIndex(@NotNull LuceneIndex index) {
		addIndex(index, null);
	}
	
	@ThreadSafe
	private void addIndex(	@NotNull LuceneIndex index,
							@Nullable Long lastModified) {
		Util.checkNotNull(index);
		Util.checkNotNull(index.getIndexDir()); // RAM indexes not allowed
		writeLock.lock();
		try {
			if (indexes.containsKey(index))
				return;
			indexes.put(index, lastModified);
		}
		finally {
			writeLock.unlock();
		}
		evtAdded.fire(index);
	}
	
	@ThreadSafe
	public void removeIndexes(	@NotNull Collection<LuceneIndex> indexesToRemove,
								boolean deleteFiles) {
		Util.checkNotNull(indexesToRemove);
		if (indexesToRemove.isEmpty())
			return; // Avoid firing event when given collection is empty
		
		int size = indexesToRemove.size();
		List<LuceneIndex> removed = new ArrayList<LuceneIndex>(size);
		List<PendingDeletion> deletions = deleteFiles
			? new ArrayList<PendingDeletion>(size)
			: null;
		
		writeLock.lock();
		try {
			for (LuceneIndex index : indexesToRemove) {
				if (!indexes.containsKey(index))
					continue;
				indexes.remove(index);
				if (deleteFiles)
					deletions.add(new PendingDeletion(index));
				removed.add(index);
			}

			/*
			 * This is done with the lock held to avoid releasing and
			 * reacquiring it.
			 */
			if (deletions != null) {
				queue.approveDeletions(deletions);
				searcher.get().approveDeletions(deletions);
			}
		}
		finally {
			writeLock.unlock();
		}
		
		evtRemoved.fire(removed);
	}

	// Allows attaching change listeners and processing the existing indexes in
	// one atomic operation, i.e. the indexes handler only receives the indexes
	// that will existed when this method is called.
	// Indexes handler and listeners are notified *without* holding the lock.
	// Events may arrive from non-GUI threads; indexes handler runs in the same
	// thread as the client
	// The list of indexes given to the handler is an immutable copy
	@ThreadSafe
	public void addListeners(	@NotNull ExistingIndexesHandler handler,
								@Nullable Event.Listener<LuceneIndex> addedListener,
								@Nullable Event.Listener<List<LuceneIndex>> removedListener) {
		Util.checkNotNull(handler);
		List<LuceneIndex> indexesCopy;
		writeLock.lock();
		try {
			indexesCopy = ImmutableList.copyOf(indexes.keySet());
			if (addedListener != null)
				evtAdded.add(addedListener);
			if (removedListener != null)
				evtRemoved.add(removedListener);
		}
		finally {
			writeLock.unlock();
		}
		handler.handleExistingIndexes(indexesCopy);
	}
	
	@ThreadSafe
	public void removeListeners(@Nullable Event.Listener<LuceneIndex> addedListener,
								@Nullable Event.Listener<List<LuceneIndex>> removedListener) {
		if (addedListener == null && removedListener == null)
			return;
		
		/*
		 * The event class is thread-safe; the lock is only used to make this an
		 * atomic operation.
		 */
		writeLock.lock();
		try {
			if (addedListener != null)
				evtAdded.remove(addedListener);
			if (removedListener != null)
				evtRemoved.remove(removedListener);
		}
		finally {
			writeLock.unlock();
		}
	}

	@ImmutableCopy
	@NotNull
	@ThreadSafe
	public List<LuceneIndex> getIndexes() {
		readLock.lock();
		try {
			return ImmutableList.copyOf(indexes.keySet());
		}
		finally {
			readLock.unlock();
		}
	}

	@CallOnce
	@ThreadSafe
	public void load(@NotNull Cancelable cancelable) throws IOException {
		/*
		 * Note: To allow running this method in parallel with other operations,
		 * it is important not to lock the entire method. Otherwise, if a client
		 * tries to attach listeners to the registry while this method is
		 * running, the former would block until the latter has finished,
		 * resulting in a serialization of both operations.
		 */
		
		// Ensure this method can only called once
		Util.checkThat(searcher.isNull());
		
		indexParentDir.mkdirs(); // Needed for the folder watching
		
		for (File indexDir : Util.listFiles(indexParentDir)) {
			if (cancelable.isCanceled())
				break;
			if (!indexDir.isDirectory())
				continue;
			File serFile = new File(indexDir, SER_FILENAME);
			if (!serFile.isFile())
				continue;
			loadIndex(serFile);
		}
		
		searcher.set(new Searcher(this, fileFactory, outlookMailFactory));
		
		// Watch index directory for changes
		try {
			final DelayedExecutor executor = new DelayedExecutor(1000);
			
			final int watchId = new SimpleJNotifyListener() {
				protected void handleEvent(File targetFile, EventType eventType) {
					if (!targetFile.getName().equals(SER_FILENAME))
						return;
					if (eventType != EventType.MODIFIED)
						return;
					executor.schedule(new Runnable() {
						public void run() {
							reload();
						}
					});
				}
			}.addWatch(indexParentDir);
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						JNotify.removeWatch(watchId);
					}
					catch (JNotifyException e) {
						Util.printErr(e);
					}
				}
			});
		}
		catch (JNotifyException e) {
			Util.printErr(e);
		}
	}
	
	@ThreadSafe
	private void loadIndex(@NotNull File serFile) {
		ObjectInputStream in = null;
		try {
			FileInputStream fin = new FileInputStream(serFile);
			FileLock lock = fin.getChannel().lock(0, Long.MAX_VALUE, true);
			LuceneIndex index;
			try {
				in = new ObjectInputStream(fin);
				index = (LuceneIndex) in.readObject();
			}
			finally {
				lock.release();
			}
			addIndex(index, serFile.lastModified());
		}
		catch (Exception e) {
			Util.printErr(e); // The average user doesn't need to know
		}
		finally {
			Closeables.closeQuietly(in);
		}
	}
	
	private void reload() {
		writeLock.lock();
		try {
			Map<File, LuceneIndex> indexDirMap = Maps.newHashMap();
			for (LuceneIndex index : indexes.keySet())
				indexDirMap.put(index.getIndexDir(), index);
			
			/*
			 * The code below is pretty inefficient if many indexes are added,
			 * modified and/or removed. However, we can assume that these operations
			 * are usually performed one index at a time, so the inefficiency
			 * doesn't really matter.
			 */
			
			for (File indexDir : Util.listFiles(indexParentDir)) {
				if (!indexDir.isDirectory())
					continue;
				File serFile = new File(indexDir, SER_FILENAME);
				if (!serFile.isFile())
					continue;
				
				LuceneIndex index = indexDirMap.remove(indexDir);
				
				// New index found
				if (index == null) {
					loadIndex(serFile);
				}
				// Existing index; may have been modified
				else {
					Long oldLM = indexes.get(index);
					long newLM = serFile.lastModified();
					if (oldLM != null && oldLM.longValue() != newLM) {
						/*
						 * Remove the old version of the index and add the new
						 * version. Let's just hope it isn't in the queue or being
						 * searched in right now.
						 */
						removeIndexes(Collections.singletonList(index), false);
						loadIndex(serFile);
					}
				}
			}
			
			// Handle missing indexes
			removeIndexes(indexDirMap.values(), false);
		}
		finally {
			writeLock.unlock();
		}
	}
	
	@VisibleForPackageGroup
	public void save(@NotNull LuceneIndex index) {
		Util.checkNotNull(index);
		writeLock.lock();
		try {
			File indexDir = index.getIndexDir();
			indexDir.mkdirs();
			File serFile = new File(indexDir, SER_FILENAME);
			
			/*
			 * DocFetcher might have been burned onto a CD-ROM; if so, then just
			 * ignore it.
			 */
			if (serFile.exists() && !serFile.canWrite())
				return;
			
			ObjectOutputStream out = null;
			try {
				serFile.createNewFile();
				FileOutputStream fout = new FileOutputStream(serFile);
				FileLock lock = fout.getChannel().lock();
				try {
					out = new ObjectOutputStream(fout);
					out.writeObject(index);
				}
				finally {
					lock.release();
				}
			}
			catch (IOException e) {
				Util.printErr(e); // The average user doesn't need to know
			}
			finally {
				Closeables.closeQuietly(out);
			}
			
			// Update cached last-modified value of index
			indexes.put(index, serFile.lastModified());
			
			// TODO Write indexes.txt file used by the daemon
		}
		finally {
			writeLock.unlock();
		}
	}
	
	@NotNull
	@ThreadSafe
	public TreeCheckState getTreeCheckState() {
		// Make local copy of indexes for thread-safety
		List<LuceneIndex> localIndexes = getIndexes();
		
		TreeCheckState totalState = new TreeCheckState();
		for (LuceneIndex index : localIndexes) {
			TreeCheckState indexState = index.getTreeCheckState();
			totalState.checkedPaths.addAll(indexState.checkedPaths);
			totalState.folderCount += indexState.folderCount;
		}
		return totalState;
	}

	private static class IndexComparator implements Comparator<LuceneIndex> {
		private static final IndexComparator instance = new IndexComparator();

		public int compare(LuceneIndex o1, LuceneIndex o2) {
			// TODO use alphanum comparator
			return o1.getDisplayName().compareTo(o2.getDisplayName());
		}
	}
	
}
