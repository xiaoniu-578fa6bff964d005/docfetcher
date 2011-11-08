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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import net.sourceforge.docfetcher.model.index.IndexingQueue;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * @author Tran Nam Quang
 */
public final class Daemon {
	
	private final IndexRegistry indexRegistry;
	private final File indexesFile;

	public Daemon(@NotNull IndexRegistry indexRegistry) {
		Util.checkNotNull(indexRegistry);
		this.indexRegistry = indexRegistry;
		
		File indexParentDir = indexRegistry.getIndexParentDir();
		indexesFile = new File(indexParentDir, ".indexes.txt");
		
		/*
		 * Open a FileOutputStream for writing. This lets the daemon know that
		 * DocFetcher is currently running.
		 */
		String lockPath = Util.getAbsPath(indexesFile) + ".lock";
		try {
			final FileOutputStream daemonLock = new FileOutputStream(lockPath);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					Closeables.closeQuietly(daemonLock);
				}
			});
		}
		catch (FileNotFoundException e) {
			/*
			 * This can occur if two instances are running, if someone is using
			 * the file or if DocFetcher is run from a CD-ROM.
			 */
			Util.printErr(e);
		}
	}
	
	/**
	 * Checks if the daemon has detected changes in the indexed folders and runs
	 * index updates on all changed folders.
	 */
	public void enqueueUpdateTasks() {
		if (!indexesFile.exists())
			return; // Happens when we're inside the IDE
		
		final IndexingQueue queue = indexRegistry.getQueue();
		try {
			Files.readLines(indexesFile, Charsets.UTF_8, new LineProcessor<Void>() {
				public boolean processLine(String line) throws IOException {
					// Ignore comment lines
					if (line.startsWith("//")) //$NON-NLS-1$
						return true;
					
					// Ignore unchanged directories
					if (!line.startsWith("#")) //$NON-NLS-1$
						return true;
					
					String rootPath = line.substring(1);
					LuceneIndex index = findIndex(rootPath);
					if (index == null)
						return true; // Unknown directory?
					
					queue.addTask(index, IndexAction.UPDATE);
					return true;
				}
				public Void getResult() {
					return null;
				}
			});
		}
		catch (Exception e) {
			// Don't show stacktrace window here, GUI might not be available
			Util.printErr(e);
		}
	}
	
	@Nullable
	private LuceneIndex findIndex(@NotNull String rootPath) {
		File rootFile = new File(rootPath).getAbsoluteFile();
		for (LuceneIndex index : indexRegistry.getIndexes())
			if (rootFile.equals(index.getCanonicalRootFile()))
				return index;
		return null;
	}
	
	public void writeIndexesToFile() {
		FileWriter writer = null;
		try {
			writer = new FileWriter(indexesFile);
			for (LuceneIndex index : indexRegistry.getIndexes()) {
				writer.write(index.getCanonicalRootFile().getPath());
				writer.write(Util.LS);
			}
		}
		catch (IOException e) {
			Util.printErr(e);
		}
		finally {
			Closeables.closeQuietly(writer);
		}
	}

}
