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
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import net.contentobjects.jnotify.JNotify;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.gui.ManualLocator;
import net.sourceforge.docfetcher.model.IndexRegistry.ExistingIndexesHandler;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.index.file.FileDocument;
import net.sourceforge.docfetcher.model.index.file.FileFolder;
import net.sourceforge.docfetcher.model.index.file.FileIndex;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.concurrent.DelayedExecutor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * @author Tran Nam Quang
 */
public final class FolderWatcher {
	
	public final Event<String> evtWatchLimitError = new Event<String>();
	
	/**
	 * The watch queue is a collection of indexes to be processed by the worker
	 * thread. The boolean map value indicates whether the index should be
	 * watched or unwatched.
	 */
	private final Map<LuceneIndex, Boolean> watchQueue = Maps.newLinkedHashMap(); // guarded by lock
	
	// Should only be accessed from the worker thread
	private final Map<LuceneIndex, Integer> watchIdMap = Maps.newHashMap();

	private final Lock writeLock;
	private final Condition needsUpdate;
	private final Thread thread;
	private volatile boolean shutdown = false;
	
	private final IndexRegistry indexRegistry;
	private Event.Listener<LuceneIndex> addedListener;
	private Event.Listener<List<LuceneIndex>> removedListener;
	private Event.Listener<LuceneIndex> watchChangedListener;
	
	public FolderWatcher(@NotNull IndexRegistry indexRegistry) {
		this.indexRegistry = Util.checkNotNull(indexRegistry);
		
		writeLock = indexRegistry.getWriteLock();
		needsUpdate = writeLock.newCondition();
		
		initListeners();
		
		/*
		 * Adding and removing watches is done in a dedicated thread because
		 * adding watches can be a time-consuming operation, depending on the
		 * depth of the tree to watch.
		 */
		thread = new Thread(FolderWatcher.class.getName()) {
			public void run() {
				while (true) {
					try {
						threadLoop();
					}
					catch (InterruptedException e) {
						break;
					}
				}
			}
		};
		thread.start();
	}

	private void initListeners() {
		// Registering the listeners must be done before starting the thread
		if (thread != null)
			throw new IllegalStateException();
		
		addedListener = new Event.Listener<LuceneIndex>() {
			public void update(LuceneIndex index) {
				writeLock.lock();
				try {
					if (index.isWatchFolders()) {
						watchQueue.put(index, true);
						needsUpdate.signal();
					}
				}
				finally {
					writeLock.unlock();
				}
			}
		};
		
		removedListener = new Event.Listener<List<LuceneIndex>>() {
			public void update(List<LuceneIndex> indexes) {
				writeLock.lock();
				try {
					boolean shouldSignal = false;
					for (LuceneIndex index : indexes) {
						if (index.isWatchFolders()) {
							watchQueue.put(index, false);
							shouldSignal = true;
						}
					}
					if (shouldSignal)
						needsUpdate.signal();
				}
				finally {
					writeLock.unlock();
				}
			}
		};

		/*
		 * This lock could be moved into the indexes handler, but we'll put it
		 * here to avoid releasing and reacquiring it.
		 */
		writeLock.lock();
		try {
			indexRegistry.addListeners(new ExistingIndexesHandler() {
				public void handleExistingIndexes(List<LuceneIndex> indexes) {
					for (LuceneIndex index : indexes)
						if (index.isWatchFolders())
							watchQueue.put(index, true);
				}
			}, addedListener, removedListener);
		}
		finally {
			writeLock.unlock();
		}
		
		// React to changes to the indexes' watch flags
		watchChangedListener = new Event.Listener<LuceneIndex>() {
			public void update(LuceneIndex index) {
				writeLock.lock();
				try {
					watchQueue.put(index, index.isWatchFolders());
					needsUpdate.signal();
				}
				finally {
					writeLock.unlock();
				}
			}
		};
		LuceneIndex.evtWatchFoldersChanged.add(watchChangedListener);
	}

	private void threadLoop() throws InterruptedException {
		Map<LuceneIndex, Boolean> watchQueueCopy;
		writeLock.lock();
		try {
			while (watchQueue.isEmpty() && !shutdown)
				needsUpdate.await();
			watchQueueCopy = ImmutableMap.copyOf(watchQueue);
			watchQueue.clear();
		}
		finally {
			writeLock.unlock();
		}
		
		/*
		 * If the shutdown flag is set, ignore the watch queue. Instead, just
		 * remove all existing watches and terminate. Note that no lock is
		 * needed when accessing the shutdown flag, since its value can only
		 * change from false to true.
		 */
		if (shutdown) {
			for (Integer id : watchIdMap.values()) {
				try {
					JNotify.removeWatch(id);
				}
				catch (Exception e) {
					Util.printErr(e);
				}
			}
			watchIdMap.clear();
			LuceneIndex.evtWatchFoldersChanged.remove(watchChangedListener);
			throw new InterruptedException();
		}
		
		for (LuceneIndex index : watchQueueCopy.keySet()) {
			File rootFile = index.getCanonicalRootFile();
			
			// Don't watch read-only files (some might be on a CD-ROM)
			if (!rootFile.canWrite())
				continue;
			
			/*
			 * Note: Before adding or removing a watch, we must check
			 * whether the watch ID map already contains or doesn't contain
			 * the index as a key, respectively. Theoretically, this could
			 * happen if the watch state is 'flipped' forth and back before
			 * the worker thread processes the change. For example, an index
			 * that is already being watched could quickly flip from
			 * 'watched' to 'unwatched' and then back to 'watched'. Without
			 * looking at the watch ID map, it would then appear that we
			 * need to add a watch for the index, even though we're already
			 * watching it.
			 */
			// Add watch
			if (watchQueueCopy.get(index)) {
				if (watchIdMap.containsKey(index))
					continue;
				if (!rootFile.exists())
					continue;

				/*
				 * Tests indicate that Linux can watch individual files, but
				 * Windows and Mac OS X can't. That means on non-Linux platforms
				 * we'll watch the parent folder instead if our target is a file
				 * (for example a PST file).
				 */
				File fileToWatch = rootFile.isFile() && !Util.IS_LINUX
					? Util.getParentFile(rootFile)
					: rootFile;

				JNotifyListenerImpl listener = new JNotifyListenerImpl(index);
				try {
					int id = listener.addWatch(fileToWatch);
					watchIdMap.put(index, id);
				}
				catch (Exception e) {
					String url = ManualLocator.getManualSubpageUrl("Watch_Limit.html");
					if (url == null) {
						url = "???";
					}
					String msg = Msg.install_watch_failed.format(
						index.getDisplayName(), url, e.getMessage());
					evtWatchLimitError.fire(msg);
				}
			}
			// Remove watch
			else {
				Integer id = watchIdMap.remove(index);
				if (id == null)
					continue;
				if (!rootFile.exists()) {
					// Remove ID from map even if root file doesn't exist
					continue;
				}
				try {
					JNotify.removeWatch(id);
				}
				catch (Exception e) {
					Util.printErr(e);
				}
			}
		}
	}
	
	public void shutdown() {
		writeLock.lock();
		try {
			if (shutdown)
				return;
			shutdown = true;
			indexRegistry.removeListeners(addedListener, removedListener);
			needsUpdate.signal(); // Might need to wake up the worker thread
		}
		finally {
			writeLock.unlock();
		}
	}
	
	private final class JNotifyListenerImpl extends SimpleJNotifyListener {
		private final LuceneIndex watchedIndex;
		private final DelayedExecutor delayedExecutor = new DelayedExecutor(1000);
		
		private JNotifyListenerImpl(@NotNull LuceneIndex watchedIndex) {
			this.watchedIndex = Util.checkNotNull(watchedIndex);
		}
		
		protected void handleEvent(File targetFile, EventType eventType) {
			if (!accept(targetFile, eventType))
				return;
			
			/*
			 * JNotify can fire many events in rapid succession, so we'll add a
			 * small delay here in order to let the file system "cool down".
			 */
			delayedExecutor.schedule(new Runnable() {
				public void run() {
					indexRegistry.getQueue().addTask(
						watchedIndex, IndexAction.UPDATE);
				}
			});
		}
		
		private boolean accept(	@NotNull File target,
								@NotNull EventType eventType) {
			String name = target.getName();
			boolean isFile = target.isFile();
			boolean isDeleted = eventType == EventType.DELETED;
			
			/*
			 * If the file was deleted, File.isFile() should have returned
			 * false, regardless of whether the file object was a file or a
			 * directory. Consequently, the isFile flag is of no use if the file
			 * was deleted.
			 */
			if (isDeleted)
				assert !isFile;
			
			/*
			 * Ignore file events originating from the index directory -- the
			 * user may have indexed the DocFetcher folder. If we don't ignore
			 * these events, we may get stuck in an infinite loop of running
			 * index updates and reacting to changes in the index directory
			 * caused by our own index updates.
			 */
			if (Util.contains(indexRegistry.getIndexParentDir(), target))
				return false;
			
			// Accept if target is an archive root or a PST file
			if (target.equals(watchedIndex.getCanonicalRootFile()))
				return true;

			// Ignore so-called 'temporary owner files' created by MS Word.
			// See bug #2804172.
			if (name.matches("~\\$.*\\.docx?"))
				return false;
			
			// Ignore target if it's matched by the user-defined filter
			IndexingConfig config = watchedIndex.getConfig();
			Path path = config.getStorablePath(target);
			
			// Apply exclusion filters
			boolean mimeMatch = false;
			outer: {
				for (PatternAction patternAction : config.getPatternActions()) {
					switch (patternAction.getAction()) {
					case EXCLUDE:
						if (!isDeleted && patternAction.matches(name, path, isFile))
							return false;
						break;
					case DETECT_MIME:
						if (patternAction.matches(name, path, isFile)) {
							mimeMatch = true;
							break outer;
						}
						break;
					default:
						throw new IllegalStateException();
					}
				}
			}
			
			// Ignore unparsable files
			if (isFile && !config.isArchive(name) && !mimeMatch
					&& !ParseService.canParseByName(config, name))
				return false;
			
			/*
			 * Check if the file was *really* modified - JNotify tends to fire
			 * even when files have only been accessed.
			 */
			if (eventType == EventType.MODIFIED
					&& (watchedIndex instanceof FileIndex)) {
				FileIndex index = (FileIndex) watchedIndex;
				TreeNode treeNode = index.getRootFolder().findTreeNode(path);
				if (sameLastModified(treeNode, target))
					return false;
			}
			
			return true;
		}

		private boolean sameLastModified(	@Nullable TreeNode treeNode,
											@NotNull File file) {
			if (treeNode == null)
				return false;
			if (treeNode instanceof FileDocument) {
				FileDocument doc = (FileDocument) treeNode;
				return doc.getLastModified() == file.lastModified();
			}
			if (treeNode instanceof FileFolder) {
				// Implicit assertion: tree node is an archive
				FileFolder folder = (FileFolder) treeNode;
				return folder.getLastModified().longValue() == file.lastModified();
			}
			throw new IllegalStateException();
		}
	};
	
}
