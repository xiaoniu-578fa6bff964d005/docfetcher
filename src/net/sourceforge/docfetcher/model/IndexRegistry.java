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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.Util;
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
	private final Event<LuceneIndex> evtAdded = new Event<LuceneIndex>();
	private final Event<List<LuceneIndex>> evtRemoved = new Event<List<LuceneIndex>>();

	private final File indexParentDir;
	private final List<LuceneIndex> indexes = new ArrayList<LuceneIndex>();
	private final IndexingQueue queue;
	private final HotColdFileCache unpackCache;
	private final FileFactory fileFactory;
	private final OutlookMailFactory outlookMailFactory;
	
	@Nullable private Searcher searcher;

	public IndexRegistry(	@NotNull File indexParentDir,
							int cacheSize,
							int reporterCapacity) {
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
	public File getIndexParentDir() {
		return indexParentDir;
	}
	
	@NotNull
	public IndexingQueue getQueue() {
		return queue;
	}
	
	// will return null until load(...) is called
	@Nullable
	public Searcher getSearcher() {
		return searcher;
	}

	@NotNull
	@VisibleForPackageGroup
	public synchronized void addIndex(@NotNull LuceneIndex index) {
		Util.checkNotNull(index);
		if (indexes.contains(index)) return;
		indexes.add(index);
		Collections.sort(indexes, IndexComparator.instance);
		evtAdded.fire(index);
	}
	
	public synchronized void removeIndexes(	@NotNull Collection<LuceneIndex> indexesToRemove,
											boolean deleteFiles) {
		Util.checkNotNull(indexesToRemove);
		if (indexesToRemove.isEmpty())
			return; // Avoid firing event when given collection is empty
		List<LuceneIndex> removed = new ArrayList<LuceneIndex>(
			indexesToRemove.size());
		for (LuceneIndex index : indexesToRemove) {
			if (!indexes.remove(index))
				continue;
			if (deleteFiles)
				index.delete();
			removed.add(index);
		}
		evtRemoved.fire(removed);
	}

	// Allows attaching a change listener and processing the existing indexes in
	// one atomic operation.
	// Note: The IndexRegistry is locked when the event handlers are notified,
	// so that the IndexRegistry cannot change during the execution of the event handler
	// Events may arrive from non-GUI threads; indexes handler runs in the same
	// thread as the client
	// The list of indexes given to the handler is an immutable copy
	public synchronized void addListeners(	@NotNull ExistingIndexesHandler handler,
											@NotNull Event.Listener<LuceneIndex> addedListener,
											@NotNull Event.Listener<List<LuceneIndex>> removedListener) {
		Util.checkNotNull(handler, addedListener, removedListener);
		handler.handleExistingIndexes(ImmutableList.copyOf(indexes));
		evtAdded.add(addedListener);
		evtRemoved.add(removedListener);
	}
	
	public synchronized void removeListeners(	@NotNull Event.Listener<LuceneIndex> addedListener,
												@NotNull Event.Listener<List<LuceneIndex>> removedListener) {
		Util.checkNotNull(addedListener, removedListener);
		evtAdded.remove(addedListener);
		evtRemoved.remove(removedListener);
	}

	@ImmutableCopy
	@NotNull
	public synchronized List<LuceneIndex> getIndexes() {
		return ImmutableList.copyOf(indexes);
	}

	// should only be called once
	public synchronized void load(@NotNull Cancelable cancelable)
			throws IOException {
		Util.checkThat(searcher == null);
		for (File indexDir : Util.listFiles(indexParentDir)) {
			if (cancelable.isCanceled()) break;
			if (!indexDir.isDirectory()) continue;
			File serFile = new File(indexDir, SER_FILENAME);
			if (!serFile.isFile()) continue;
			ObjectInputStream stream = null;
			try {
				stream = new ObjectInputStream(new FileInputStream(serFile));
				LuceneIndex index = (LuceneIndex) stream.readObject();
				addIndex(index);
			}
			catch (Exception e) {
				Util.printErr(e); // The average user doesn't need to know
			}
			finally {
				Closeables.closeQuietly(stream);
			}
		}
		searcher = new Searcher(this, fileFactory, outlookMailFactory);
	}

	public synchronized void save() {
		for (LuceneIndex index : indexes)
			saveUnchecked(index);

		// TODO Write indexes.txt file used by the daemon
		// TODO Method is currently not in use
	}
	
	@VisibleForPackageGroup
	public synchronized void save(@NotNull LuceneIndex index) {
		Util.checkNotNull(index);
		Util.checkThat(indexes.contains(index));
		saveUnchecked(index);
	}
	
	private void saveUnchecked(@NotNull LuceneIndex index) {
		File indexDir = index.getIndexDir();
		indexDir.mkdirs();
		File serFile = new File(indexDir, SER_FILENAME);

		/*
		 * DocFetcher might have been burned onto a CD-ROM; if so, then just
		 * ignore it.
		 */
		if (serFile.exists() && !serFile.canWrite()) return;

		ObjectOutputStream stream = null;
		try {
			serFile.createNewFile();
			stream = new ObjectOutputStream(new FileOutputStream(serFile));
			stream.writeObject(index);
		}
		catch (IOException e) {
			Util.printErr(e); // The average user doesn't need to know
		}
		finally {
			Closeables.closeQuietly(stream);
		}
	}
	
	@NotNull
	@ThreadSafe
	public TreeCheckState getTreeCheckState() {
		List<LuceneIndex> localIndexes;
		synchronized (this) {
			localIndexes = new ArrayList<LuceneIndex>(indexes);
		}
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
