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
import net.sourceforge.docfetcher.base.annotations.Immutable;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.ThreadSafe;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.UtilModel.QueryWrapper;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingQueue;
import net.sourceforge.docfetcher.model.index.file.FileFactory;
import net.sourceforge.docfetcher.model.index.outlook.OutlookMailFactory;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.model.search.SearchException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.Version;

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
	public synchronized void addListeners(	@NotNull ExistingIndexesHandler handler,
											@NotNull Event.Listener<LuceneIndex> addedListener,
											@NotNull Event.Listener<List<LuceneIndex>> removedListener) {
		Util.checkNotNull(handler, addedListener, removedListener);
		handler.handleExistingIndexes(getIndexes());
		evtAdded.add(addedListener);
		evtRemoved.add(removedListener);
	}
	
	public synchronized void removeListeners(	@NotNull Event.Listener<LuceneIndex> addedListener,
												@NotNull Event.Listener<List<LuceneIndex>> removedListener) {
		Util.checkNotNull(addedListener, removedListener);
		evtAdded.remove(addedListener);
		evtRemoved.remove(removedListener);
	}

	@Immutable
	@NotNull
	public synchronized List<LuceneIndex> getIndexes() {
		return Collections.unmodifiableList(indexes);
	}

	public synchronized void load(@NotNull Cancelable cancelable) {
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
	
	@NotNull
	@VisibleForPackageGroup
	public FileFactory getFileFactory() {
		return fileFactory;
	}
	
	@NotNull
	@VisibleForPackageGroup
	public OutlookMailFactory getOutlookMailFactory() {
		return outlookMailFactory;
	}

	// TODO provide search method for web interface (must be callable from many
	// threads simultaneously!)
	// take extra care with synchronization and index deletion!!!
	// doc: Using read-only IndexSearcher for better concurrent performance

	@NotNull
	public List<ResultDocument> search(@NotNull String queryString)
			throws SearchException {
		/*
		 * Note: For the desktop interface, we'll always search in all available
		 * indexes, even those which are unchecked on the filter panel. This
		 * allows the user to re-check the unchecked indexes and see previously
		 * hidden results without starting another search.
		 */

		// Make local copy of indexes for thread-safety
		List<LuceneIndex> localIndexes;
		synchronized (this) {
			localIndexes = new ArrayList<LuceneIndex>(indexes);
		}

		// Abort if there's nothing to search in
		if (localIndexes.isEmpty()) return Collections.emptyList();

		// Check that all indexes still exist
		for (LuceneIndex index : localIndexes) {
			File indexDir = index.getIndexDir();
			if (indexDir != null && !indexDir.isDirectory()) {
				String msg = "folders_not_found"; // TODO i18n
				msg += "\n" + Util.getSystemAbsPath(indexDir);
				throw new SearchException(msg);
			}
		}

		// Create Lucene query
		QueryWrapper queryWrapper = UtilModel.createQuery(queryString);
		Query query = queryWrapper.query;
		boolean isPhraseQuery = queryWrapper.isPhraseQuery;

		/*
		 * Notes regarding the following code:
		 * 
		 * 1) Lucene will throw an IOException if the user deletes one or more
		 * indexes while a search is running over the affected indexes. The
		 * IOException is the expected behavior here, and we'll just let it
		 * happen, rather than try hard to avoid it. Avoidance may in fact be
		 * very difficult, if not impossible, when multiple DocFetcher instances
		 * are involved (e.g. one instance deleting an index, the others running
		 * searches), because in that case in-memory locks won't help.
		 * 
		 * 2) All the information needed for displaying the results must be
		 * loaded and returned immediately rather than lazily, because after the
		 * search the user might delete one or more indexes. This also means the
		 * result documents must not access the indexes later on.
		 */

		MultiSearcher searcher = null;
		try {
			// Perform search
			// TODO calling search can throw an OutOfMemoryError -> catch it
			searcher = UtilModel.createLuceneSearcher(localIndexes);
			int maxResults = ProgramConf.Int.MaxResultsTotal.get();
			ScoreDoc[] scoreDocs = searcher.search(query, maxResults).scoreDocs;

			// Create and return results
			List<ResultDocument> results = new ArrayList<ResultDocument>(
				scoreDocs.length);
			for (int i = 0; i < scoreDocs.length; i++) {
				Document doc = searcher.doc(scoreDocs[i].doc);
				float score = scoreDocs[i].score;
				LuceneIndex index = localIndexes.get(searcher.subSearcher(i));
				IndexingConfig config = index.getConfig();
				results.add(new ResultDocument(
					doc, score, query, isPhraseQuery, config, fileFactory,
					outlookMailFactory));
			}
			return results;
		}
		catch (IOException e) {
			throw new SearchException(e.getMessage()); // TODO i18n
		}
		finally {
			// This will close all searchables
			Closeables.closeQuietly(searcher);
		}
	}
	
	private static class IndexComparator implements Comparator<LuceneIndex> {
		private static final IndexComparator instance = new IndexComparator();

		public int compare(LuceneIndex o1, LuceneIndex o2) {
			// TODO use alphanum comparator
			return o1.getDisplayName().compareTo(o2.getDisplayName());
		}
	}
	
}
