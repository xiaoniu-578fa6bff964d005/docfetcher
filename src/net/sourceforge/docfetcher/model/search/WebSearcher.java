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

package net.sourceforge.docfetcher.model.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.ThreadSafe;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.Field;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.IndexRegistry.AddedEvent;
import net.sourceforge.docfetcher.model.IndexRegistry.ExistingIndexesHandler;
import net.sourceforge.docfetcher.model.IndexRegistry.RemovedEvent;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.UtilModel.QueryWrapper;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.file.FileFactory;
import net.sourceforge.docfetcher.model.index.outlook.OutlookMailFactory;
import net.sourceforge.docfetcher.model.parse.Parser;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.misc.ChainedFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.search.TopDocs;

import com.google.common.io.Closeables;

/**
 * A search API on top of the index registry, optimized for web-search-like
 * usage. This class is completely thread-safe, so usually only one instance of
 * it is needed for handling concurrent search requests.
 * <p>
 * <b>Important</b>: Instances of this class must be disposed after usage by
 * calling {@link #dispose()}.
 * 
 * @author Tran Nam Quang
 */
@ThreadSafe
public final class WebSearcher {
	
	/**
	 * A single page of results.
	 */
	public static class ResultPage {
		/** The result documents for this page. */
		public final List<ResultDocument> resultDocuments;
		
		/** The zero-based index of this page. */
		public final int pageIndex;
		
		/** The total number of pages. */
		public final int pageCount;
		
		/** The total number of result documents across all pages. */
		public final int hitCount;
		
		private ResultPage(	@NotNull List<ResultDocument> resultDocuments,
							int pageIndex,
							int pageCount,
							int hitCount) {
			this.resultDocuments = Util.checkNotNull(resultDocuments);
			this.pageIndex = pageIndex;
			this.pageCount = pageCount;
			this.hitCount = hitCount;
		}
	}
	
	private static final int PAGE_SIZE = Math.min(1, ProgramConf.Int.WebInterfacePageSize.get());
	
	private final IndexRegistry indexRegistry;
	private final Event.Listener<AddedEvent> addedListener;
	private final Event.Listener<RemovedEvent> removedListener;
	
	@NotNull
	private MultiSearcher searcher;
	@NotNull
	private List<LuceneIndex> indexes;
	@Nullable
	private volatile IOException ioException;
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();
	
	public WebSearcher(@NotNull IndexRegistry indexRegistry) throws IOException {
		this.indexRegistry = Util.checkNotNull(indexRegistry);
		
		addedListener = new Event.Listener<AddedEvent>() {
			public void update(AddedEvent eventData) {
				replaceLuceneSearcher();
			}
		};
		removedListener = new Event.Listener<RemovedEvent>() {
			public void update(RemovedEvent eventData) {
				replaceLuceneSearcher();
			}
		};
		
		indexRegistry.addListeners(new ExistingIndexesHandler() {
			public void handleExistingIndexes(List<LuceneIndex> indexes) {
				try {
					searcher = UtilModel.createLuceneSearcher(indexes);
					WebSearcher.this.indexes = indexes;
				}
				catch (IOException e) {
					ioException = e;
				}
			}
		}, addedListener, removedListener);
		
		if (ioException != null)
			throw ioException;
	}
	
	/**
	 * Updates the cached indexes and replaces the current Lucene searcher with
	 * a new one.
	 */
	@ThreadSafe
	private void replaceLuceneSearcher() {
		writeLock.lock();
		try {
			Closeables.close(searcher, false);
			
			// Set the indexes *before* creating the searcher
			indexes = indexRegistry.getIndexes();
			searcher = UtilModel.createLuceneSearcher(indexes);
		}
		catch (IOException e) {
			ioException = e; // Will be thrown later
		}
		finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * For the given query, returns the requested page of results. This method
	 * should not be called anymore after {@link #dispose()} has been called,
	 * otherwise an IOException will be thrown.
	 */
	@NotNull
	@ThreadSafe
	public ResultPage submit(@NotNull WebQuery webQuery)
			throws IOException, SearchException {
		Util.checkNotNull(webQuery);
		
		if (ioException != null)
			throw ioException;
		
		List<Filter> filters = new ArrayList<Filter>(3);
		
		// Add size filter to filter chain
		if (webQuery.minSize != null || webQuery.maxSize != null) {
			filters.add(NumericRangeFilter.newLongRange(
				Field.SIZE.key(), webQuery.minSize, webQuery.maxSize, true,
				true));
		}
		
		// Add type filter to filter chain
		if (webQuery.parsers != null) {
			TermsFilter typeFilter = new TermsFilter();
			String fieldName = Field.PARSER.key();
			typeFilter.addTerm(new Term(fieldName, Field.EMAIL_PARSER));
			for (Parser parser : webQuery.parsers) {
				String parserName = parser.getClass().getSimpleName();
				typeFilter.addTerm(new Term(fieldName, parserName));
			}
			filters.add(typeFilter);
		}
		
		// Add location filter to filter chain
		if (webQuery.indexes != null) {
			Filter[] indexFilters = new Filter[webQuery.indexes.size()];
			int i = 0;
			for (LuceneIndex index : webQuery.indexes) {
				String path = index.getRootFile().getPath();
				String uid = index.getDocumentType().createUniqueId(path);
				Term prefix = new Term(Field.UID.key(), uid + "/");
				indexFilters[i++] = new PrefixFilter(prefix);
			}
			filters.add(new ChainedFilter(indexFilters, ChainedFilter.OR));
		}
		
		// Construct filter chain
		Filter filter = filters.size() == 0 ? null : new ChainedFilter(
			filters.toArray(new Filter[filters.size()]), ChainedFilter.AND);
		
		// Create query
		QueryWrapper queryWrapper = UtilModel.createQuery(webQuery.query);
		Query query = queryWrapper.query;
		boolean isPhraseQuery = queryWrapper.isPhraseQuery;
		
		readLock.lock();
		try {
			// Perform search
			int maxResults = (webQuery.pageIndex + 1) * PAGE_SIZE;
			TopDocs topDocs = searcher.search(query, filter, maxResults);
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			
			FileFactory fileFactory = indexRegistry.getFileFactory();
			OutlookMailFactory outlookMailFactory = indexRegistry.getOutlookMailFactory();
			
			// Compute start and end indices of returned page
			int start;
			int end = scoreDocs.length;
			if (end <= PAGE_SIZE) {
				start = 0;
			}
			else {
				int r = end % PAGE_SIZE;
				start = end - (r == 0 ? PAGE_SIZE : r);
			}

			// Create and fill list of result documents to return
			List<ResultDocument> results = new ArrayList<ResultDocument>(end
					- start);
			for (int i = start; i < end; i++) {
				Document doc = searcher.doc(scoreDocs[i].doc);
				float score = scoreDocs[i].score;
				LuceneIndex index = indexes.get(searcher.subSearcher(i));
				IndexingConfig config = index.getConfig();
				results.add(new ResultDocument(
					doc, score, query, isPhraseQuery, config, fileFactory,
					outlookMailFactory));
			}
			
			int hitCount = topDocs.totalHits;
			int newPageIndex = start / PAGE_SIZE;
			int pageCount = (int) Math.ceil((float) hitCount / PAGE_SIZE);
			
			return new ResultPage(results, newPageIndex, pageCount, hitCount);
		}
		finally {
			readLock.unlock();
		}
	}

	/**
	 * Disposes of the receiver. The caller should make sure that no more search
	 * requests are submitted to the receiver after this method is called.
	 */
	@ThreadSafe
	public void dispose() {
		if (ioException != null)
			Util.printErr(ioException);
		
		writeLock.lock();
		try {
			indexRegistry.removeListeners(addedListener, removedListener);
			Closeables.closeQuietly(searcher);
		}
		finally {
			writeLock.unlock();
		}
	}

}
