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
import java.util.concurrent.locks.ReentrantLock;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;
import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.base.DelayedExecutor;
import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.model.IndexRegistry.ExistingIndexesHandler;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.index.file.FileDocument;
import net.sourceforge.docfetcher.model.index.file.FileIndex;
import net.sourceforge.docfetcher.model.parse.ParseService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * @author Tran Nam Quang
 */
public final class FolderWatcher {
	
	/**
	 * The watch queue is a collection of indexes to be processed by the worker
	 * thread. The boolean map value indicates whether the index should be
	 * watched or unwatched.
	 */
	private final Map<LuceneIndex, Boolean> watchQueue = Maps.newLinkedHashMap(); // guarded by lock
	
	// These two fields should only be accessed from the worker thread
	private final Map<LuceneIndex, Integer> watchIdMap = Maps.newHashMap();
	private final Map<Integer, DelayedExecutor> idsToExecutorsMap = Maps.newHashMap();
	
	private final Lock lock = new ReentrantLock(true);
	private final Condition needsUpdate = lock.newCondition();
	private final Thread thread;
	private volatile boolean shutdown = false;
	
	private final IndexRegistry indexRegistry;
	private Event.Listener<LuceneIndex> addedListener;
	private Event.Listener<List<LuceneIndex>> removedListener;
	private Event.Listener<LuceneIndex> watchChangedListener;
	
	public FolderWatcher(@NotNull IndexRegistry indexRegistry) {
		this.indexRegistry = Util.checkNotNull(indexRegistry);
		
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
				lock.lock();
				try {
					if (index.isWatchFolders()) {
						watchQueue.put(index, true);
						needsUpdate.signal();
					}
				}
				finally {
					lock.unlock();
				}
			}
		};
		removedListener = new Event.Listener<List<LuceneIndex>>() {
			public void update(List<LuceneIndex> indexes) {
				lock.lock();
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
					lock.unlock();
				}
			}
		};
		indexRegistry.addListeners(new ExistingIndexesHandler() {
			public void handleExistingIndexes(List<LuceneIndex> indexes) {
				/*
				 * Neither locking nor signalling are needed here because the
				 * worker thread hasn't been started yet.
				 */
				assert thread == null;
				for (LuceneIndex index : indexes)
					if (index.isWatchFolders())
						watchQueue.put(index, true);
			}
		}, addedListener, removedListener);
		
		// React to changes to the indexes' watch flags
		watchChangedListener = new Event.Listener<LuceneIndex>() {
			public void update(LuceneIndex index) {
				lock.lock();
				try {
					watchQueue.put(index, index.isWatchFolders());
					needsUpdate.signal();
				}
				finally {
					lock.unlock();
				}
			}
		};
		LuceneIndex.evtWatchFoldersChanged.add(watchChangedListener);
	}

	private void threadLoop() throws InterruptedException {
		Map<LuceneIndex, Boolean> watchQueueCopy;
		lock.lock();
		try {
			while (watchQueue.isEmpty() && !shutdown)
				needsUpdate.await();
			watchQueueCopy = ImmutableMap.copyOf(watchQueue);
			watchQueue.clear();
		}
		finally {
			lock.unlock();
		}
		
		/*
		 * If the shutdown flag is set, ignore the watch queue. Instead, just
		 * remove all existing watches and terminate. Note that no lock is
		 * needed when accessing the shutdown flag, since its value can only
		 * change from false to true.
		 */
		if (shutdown) {
			for (Integer id : watchIdMap.values()) {
				idsToExecutorsMap.remove(id).shutdown();
				try {
					JNotify.removeWatch(id);
				}
				catch (Exception e) {
					Util.printErr(e);
				}
			}
			assert idsToExecutorsMap.isEmpty();
			watchIdMap.clear();
			LuceneIndex.evtWatchFoldersChanged.remove(watchChangedListener);
			throw new InterruptedException();
		}
		
		for (LuceneIndex index : watchQueueCopy.keySet()) {
			File rootFile = index.getRootFile();
			
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
				 * Windows can't. That means on non-Linux platforms we'll
				 * watch the parent folder instead if our target is a file
				 * (for example a PST file).
				 * 
				 * TODO check this for Mac OS X
				 */
				File fileToWatch = rootFile.isFile() && !Util.IS_LINUX
				? Util.getParentFile(rootFile)
					: rootFile;
				String path = Util.getSystemAbsPath(fileToWatch);

				JNotifyListenerImpl listener = new JNotifyListenerImpl(index);
				try {
					int id = JNotify.addWatch(
						path, JNotify.FILE_ANY, true, listener);
					watchIdMap.put(index, id);
					idsToExecutorsMap.put(id, listener.delayedExecutor);
				}
				catch (Exception e) {
					// TODO suggest raising the global watch limit
					String msg = String.format(
						"Failed to install watch for folder '%s'.\n\n"
								+ "Internal error message:\n%s",
						index.getDisplayName(), e.getMessage());
					AppUtil.showError(msg, true, false);
				}
			}
			// Remove watch
			else {
				Integer id = watchIdMap.remove(index);
				if (id == null)
					continue;
				idsToExecutorsMap.remove(id).shutdown();
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
		lock.lock();
		try {
			if (shutdown)
				return;
			shutdown = true;
			indexRegistry.removeListeners(addedListener, removedListener);
			needsUpdate.signal(); // Might need to wake up the worker thread
		}
		finally {
			lock.unlock();
		}
	}
	
	private final class JNotifyListenerImpl implements JNotifyListener {
		private final LuceneIndex watchedIndex;
		private final DelayedExecutor delayedExecutor = new DelayedExecutor(1000);
		
		private JNotifyListenerImpl(@NotNull LuceneIndex watchedIndex) {
			this.watchedIndex = Util.checkNotNull(watchedIndex);
		}
		public void fileCreated(int wd, String rootPath, String name) {
			handleEvent(rootPath, name);
		}
		public void fileDeleted(int wd, String rootPath, String name) {
			handleEvent(rootPath, name);
		}
		public void fileModified(int wd, String rootPath, String name) {
			handleEvent(rootPath, name);
		}
		public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
			handleEvent(rootPath, newName);
		}
		private void handleEvent(String rootPath, String name) {
			if (!accept(new File(rootPath, name)))
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
		private boolean accept(@NotNull File target) {
			String name = target.getName();
			boolean isFile = target.isFile();
			
			// Accept if target is an archive root or a PST file
			if (target.equals(watchedIndex.getRootFile()))
				return true;

			// Ignore so-called 'temporary owner files' created by MS Word.
			// See bug #2804172.
			if (isFile && name.matches("~\\$.*\\.docx?"))
				return false;
			
			// Ignore target if it's matched by the user-defined filter
			IndexingConfig config = watchedIndex.getConfig();
			String path = config.getStorablePath(target);
			if (config.getFileFilter().matches(name, path, isFile))
				return false;
			
			// Ignore unparsable files
			if (isFile && !config.isArchive(name)
					&& !config.matchesMimePattern(name)
					&& !ParseService.canParseByName(config, name))
				return false;
			
			// Check if the file was *really* modified (JNotify tends to fire
			// even when files have only been accessed)
			if (watchedIndex instanceof FileIndex) {
				FileIndex index = (FileIndex) watchedIndex;
				FileDocument doc = index.getRootFolder().findDocument(path);
				if (doc != null && doc.getLastModified() == target.lastModified())
					return false;
			}
			
			return true;
		}
	};
	
}
