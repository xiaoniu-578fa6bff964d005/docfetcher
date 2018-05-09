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

package net.sourceforge.docfetcher.gui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.gui.ResultPanel.HeaderMode;
import net.sourceforge.docfetcher.gui.filter.FileTypePanel;
import net.sourceforge.docfetcher.gui.filter.FilesizePanel;
import net.sourceforge.docfetcher.gui.filter.IndexPanel;
import net.sourceforge.docfetcher.model.Fields;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.TreeCheckState;
import net.sourceforge.docfetcher.model.parse.Parser;
import net.sourceforge.docfetcher.model.search.PhraseDetectingQueryParser;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.model.search.SearchException;
import net.sourceforge.docfetcher.model.search.Searcher;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.collect.ListMap;
import net.sourceforge.docfetcher.util.collect.ListMap.Entry;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

/**
 * @author Tran Nam Quang
 */
public final class SearchQueue {
	
	private static enum GuiEvent {
		SEARCH_OR_LIST, SIZE, TYPE, LOCATION, TYPE_AHEAD,
	}
	
	private static final String spaces = Strings.repeat(" ", 5);

	private final SearchBar searchBar;
	private final FilesizePanel filesizePanel;
	private final FileTypePanel fileTypePanel;
	private final IndexPanel indexPanel;
	private final ResultPanel resultPanel;
	private final StatusBar statusBar;
	
	private final Thread thread;
	private final Lock lock = new ReentrantLock(true);
	private final Condition queueNotEmpty = lock.newCondition();
	private final EnumSet<GuiEvent> queue = EnumSet.noneOf(GuiEvent.class);
	
	@Nullable private volatile String query;
	@Nullable private volatile Set<String> listDocIds;
	@Nullable private List<ResultDocument> results;
	@Nullable private Set<String> checkedParsers;
	@Nullable private TreeCheckState treeCheckState;
	private boolean allParsersChecked;
	
	public SearchQueue(	@NotNull SearchBar searchBar,
						@NotNull FilesizePanel filesizePanel,
						@NotNull FileTypePanel fileTypePanel,
						@NotNull IndexPanel indexPanel,
						@NotNull ResultPanel resultPanel,
						@NotNull StatusBar statusBar) {
		Util.checkNotNull(searchBar, filesizePanel, fileTypePanel);
		Util.checkNotNull(indexPanel, resultPanel, statusBar);
		this.searchBar = searchBar;
		this.filesizePanel = filesizePanel;
		this.fileTypePanel = fileTypePanel;
		this.indexPanel = indexPanel;
		this.resultPanel = resultPanel;
		this.statusBar = statusBar;
		
		resultPanel.evtSelection.add(new Event.Listener<List<ResultDocument>>() {
			public void update(List<ResultDocument> eventData) {
				updateResultStatus();
			}
		});
		
		thread = new Thread(SearchQueue.class.getName()) {
			public void run() {
				while (threadLoop());
			}
		};
		thread.start();
		
		initListeners();
	}
	
	private void initListeners() {
		searchBar.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				thread.interrupt();
			}
		});
		
		searchBar.evtSearch.add(new Event.Listener<String>() {
			public void update(String eventData) {
				lock.lock();
				try {
					query = eventData;
					searchBar.setEnabled(false);
					queue.add(GuiEvent.SEARCH_OR_LIST);
					queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});

		searchBar.evtSearch_typeahead.add(new Event.Listener<String>() {
			public void update(String eventData) {
				lock.lock();
				try {
					query = eventData+'*';
					try {
						PhraseDetectingQueryParser queryParser = new PhraseDetectingQueryParser(
								Fields.CONTENT.key(), IndexRegistry.getAnalyzer());
						Query temp_query = queryParser.parse(query);
					} catch (ParseException e) {
						return;
					}
                    queue.add(GuiEvent.SEARCH_OR_LIST);
                    queue.add(GuiEvent.TYPE_AHEAD);
                    queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});

		filesizePanel.evtValuesChanged.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				lock.lock();
				try {
					queue.add(GuiEvent.SIZE);
					queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});
		
		fileTypePanel.evtCheckStatesChanged.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				lock.lock();
				try {
					queue.add(GuiEvent.TYPE);
					queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});
		
		indexPanel.evtCheckStatesChanged.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				lock.lock();
				try {
					queue.add(GuiEvent.LOCATION);
					queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});
		
		indexPanel.evtListDocuments.add(new Event.Listener<Set<String>>() {
			public void update(Set<String> eventData) {
				lock.lock();
				try {
					listDocIds = eventData;
					queue.add(GuiEvent.SEARCH_OR_LIST);
					queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});
	}
	
	private boolean threadLoop() {
		final EnumSet<GuiEvent> queueCopy;
		final String query;
		final Set<String> listDocIds;
		
		lock.lock();
		try {
			while (queue.isEmpty())
				queueNotEmpty.await();
			
			queueCopy = EnumSet.copyOf(queue);
			queue.clear();
			query = this.query;
			listDocIds = this.listDocIds;
			this.query = null;
			this.listDocIds = null;
		}
		catch (InterruptedException e) {
			return false;
		}
		finally {
			lock.unlock();
		}
		
		IndexRegistry indexRegistry = indexPanel.getIndexRegistry();

		// Run search
		if (queueCopy.contains(GuiEvent.SEARCH_OR_LIST)) {
			try {
				Searcher searcher = indexRegistry.getSearcher(); // might block
				
				/*
				 * Bug #3538102: The returned searcher is null if
				 * IndexRegistry.getSearcher() was blocking and the thread is
				 * interrupted. This can happen as follows: (1) The user has a
				 * lot of indexes and/or the indexes are very large, so that
				 * loading them on startup takes a long time. (2) During
				 * startup, when the indexes are loaded, the user enters
				 * something into the search field and presses Enter. (3)
				 * DocFetcher blocks because it can't start searching until all
				 * indexes have been loaded. Seeing that the program has
				 * apparently frozen, the user closes the program. This
				 * interrupts the searcher thread, causing the
				 * IndexRegistry.getSearcher() method to unblock and return
				 * null.
				 */
				if (searcher == null)
					return false;
				
				if (query != null)
					results = searcher.search(query);
				else if (listDocIds != null)
					results = searcher.list(listDocIds);
				else
					throw new IllegalStateException();
			}
			catch (SearchException e) {
				AppUtil.showError(e.getMessage(), true, true);
				Util.runSyncExec(searchBar.getControl(), new Runnable() {
					public void run() {
						searchBar.setEnabled(true);
					}
				});
				
				// Don't return yet, we might have to update the filters
				results = null;
			}
			catch (CheckedOutOfMemoryError e) {
				UtilGui.showOutOfMemoryMessage(searchBar.getControl(), e);
			}
		}
		
		// Build parser filter
		if (checkedParsers == null || queueCopy.contains(GuiEvent.TYPE)) {
			Util.runSyncExec(fileTypePanel.getControl(), new Runnable() {
				public void run() {
					updateParserFilter();
				}
			});
		}
		
		// Build location filter
		if (treeCheckState == null || queueCopy.contains(GuiEvent.LOCATION))
			treeCheckState = indexRegistry.getTreeCheckState();
		
		/*
		 * No need to update the result panel if the user changed the filter
		 * settings before having run any searches.
		 */
		if (results == null)
			return true;
		
		Long[] minMax = filesizePanel.getValuesInKB();
		final List<ResultDocument> visibleResults = new ArrayList<ResultDocument>();

		// Apply filters
		for (ResultDocument doc : results) {
			if (minMax != null) {
				long size = doc.getSizeInKB();
				if (minMax[0] != null && size < minMax[0])
					continue;
				if (minMax[1] != null && size > minMax[1])
					continue;
			}
			if (!doc.isEmail()) {
				if (checkedParsers.isEmpty())
					continue;
				if (!allParsersChecked) {
					String parserName = doc.getParserName();
					if (!checkedParsers.contains(parserName))
						continue;
				}
			}
			if (!treeCheckState.isChecked(doc.getParentPath()))
				continue;
			visibleResults.add(doc);
		}
		
		boolean filesFound = false;
		boolean emailsFound = false;
		for (LuceneIndex index : indexRegistry.getIndexes()) {
			if (index.isEmailIndex())
				emailsFound = true;
			else
				filesFound = true;
		}
		final HeaderMode mode = HeaderMode.getInstance(filesFound, emailsFound);
		
		// Set results
		Util.runSyncExec(searchBar.getControl(), new Runnable() {
			public void run() {
				resultPanel.setResults(visibleResults, mode);
				resultPanel.sortByColumn(ProgramConf.Int.InitialSorting.get());
				if (queueCopy.contains(GuiEvent.SEARCH_OR_LIST) && ! queueCopy.contains(GuiEvent.TYPE_AHEAD))
					resultPanel.getControl().setFocus();
				updateResultStatus(); // Must be done *after* setting the results
                searchBar.setEnabled(true);

				if (! queueCopy.contains(GuiEvent.TYPE_AHEAD)) {
					if (query != null)
						searchBar.addToSearchHistory(query);
				}
			}
		});

		return true;
	}

	private void updateParserFilter() {
		ListMap<Parser, Boolean> map = fileTypePanel.getParserStateMap();
		checkedParsers = Sets.newHashSetWithExpectedSize(map.size());
		for (Entry<Parser, Boolean> entry : map) {
			if (!entry.getValue())
				continue;
			String parserName = entry.getKey()
					.getClass()
					.getSimpleName();
			checkedParsers.add(parserName);
		}
		allParsersChecked = checkedParsers.size() == map.size();
	}
	
	private void updateResultStatus() {
		int resultCount = resultPanel.getItemCount();
		String msg = Msg.num_results.format(resultCount);
		if (resultCount >= Searcher.MAX_RESULTS)
			msg += "+";
		int selCount = resultPanel.getSelection().size();
		if (selCount > 1)
			msg += spaces + Msg.num_sel_results.format(selCount);
		statusBar.getLeftPart().setContents(Img.INFO.get(), msg);
	}

}
