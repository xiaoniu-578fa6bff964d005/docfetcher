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

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.Stoppable;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
abstract class HtmlFileLister<T extends Throwable> extends Stoppable<T> {
	
	private final File parentDir;
	private final IndexingConfig config;
	private final Collection<String> htmlExtensions;
	private final boolean htmlPairing;
	@Nullable private final IndexingReporter reporter;
	
	public HtmlFileLister(	@NotNull File parentDir,
							@NotNull IndexingConfig config,
							@Nullable IndexingReporter reporter) {
		Util.checkNotNull(parentDir, config);
		this.parentDir = parentDir;
		this.config = config;
		this.htmlExtensions = config.getHtmlExtensions();
		this.htmlPairing = config.isHtmlPairing();
		this.reporter = reporter;
	}
	
	protected final void doRun() {
		if (htmlPairing)
			runWithHtmlPairing();
		else
			runWithoutHtmlPairing();
	}
	
	private void runWithoutHtmlPairing() {
		for (File fileOrDir : Util.listFiles(parentDir)) {
			if (isStopped())
				return;
			
			boolean isFile;
			try {
				if (Util.isSymLink(fileOrDir))
					continue;
				if (skip(fileOrDir))
					continue;
				isFile = fileOrDir.isFile();
				if (ProgramConf.Bool.IgnoreJunctionsAndSymlinks.get()
						&& !isFile && Util.isJunctionOrSymlink(fileOrDir))
					continue;
			}
			catch (Throwable t) {
				handleFileException(t, fileOrDir);
				continue;
			}
			
			if (isFile) {
				if (isHtmlFile(fileOrDir))
					handleHtmlPair(fileOrDir, null);
				else
					handleFile(fileOrDir);
			} else if (fileOrDir.isDirectory()) {
				handleDir(fileOrDir);
			}
		}
	}
	
	private void runWithHtmlPairing() {
		File[] filesOrDirs = Util.listFiles(parentDir);
		if (filesOrDirs.length == 0)
			return; // Returning early avoids allocating the two lists below
		
		List<File> htmlFiles = new LinkedList<File> ();
		List<File> tempDirs = new ArrayList<File> ();
		
		// Note: The file filter should be applied *after* the HTML pairing.
		
		for (final File fileOrDir : filesOrDirs) {
			if (isStopped())
				return;
			
			boolean isFile;
			try {
				if (Util.isSymLink(fileOrDir))
					continue;
				isFile = fileOrDir.isFile();
				if (ProgramConf.Bool.IgnoreJunctionsAndSymlinks.get()
						&& !isFile && Util.isJunctionOrSymlink(fileOrDir))
					continue;
			}
			catch (Throwable t) {
				handleFileException(t, fileOrDir);
				continue;
			}
			
			if (isFile) {
				if (isHtmlFile(fileOrDir))
					htmlFiles.add(fileOrDir);
				else if (!skip(fileOrDir))
					handleFile(fileOrDir);
			}
			else if (fileOrDir.isDirectory()) {
				tempDirs.add(fileOrDir);
			}
		}
		
		/*
		 * Bug #3538230: We've already called isFile() and isDirectory() on all
		 * found files and directories in the previous loop, but we must do it
		 * again in the two following loops, because enough time may have passed
		 * due to indexing to allow the user to delete any of the files and
		 * directories from outside.
		 */
		
		for (File dirCandidate : tempDirs) {
			if (isStopped())
				return;
			String dirBasename = HtmlUtil.getHtmlDirBasename(dirCandidate);
			if (dirBasename == null) {
				if (!skip(dirCandidate) && dirCandidate.isDirectory())
					handleDir(dirCandidate);
				continue;
			}
			boolean htmlPairFound = false;
			for (Iterator<File> it = htmlFiles.iterator(); it.hasNext(); ) {
				File htmlCandidate = it.next();
				if (Util.splitFilename(htmlCandidate)[0].equals(dirBasename)) {
					if (!skip(htmlCandidate) && htmlCandidate.isFile()
							&& dirCandidate.isDirectory())
						handleHtmlPair(htmlCandidate, dirCandidate);
					it.remove();
					htmlPairFound = true;
					break;
				}
			}
			if (!htmlPairFound && !skip(dirCandidate)
					&& dirCandidate.isDirectory())
				handleDir(dirCandidate);
		}
		
		// Visit unpaired html files
		for (File htmlFile : htmlFiles) {
			if (isStopped())
				return;
			if (!skip(htmlFile) && htmlFile.isFile())
				handleHtmlPair(htmlFile, null);
		}
	}

	private boolean isHtmlFile(@NotNull File file) {
		return Util.hasExtension(file.getName(), htmlExtensions);
	}
	
	private void handleFileException(	@NotNull Throwable t,
										@NotNull final File file) {
		/*
		 * TrueZIP can throw various runtime exceptions, e.g. a
		 * CharConversionException while traversing zip files containing Chinese
		 * encodings. There was also a tar-related crash, as reported in
		 * #3436750.
		 */
		if (reporter == null) {
			Util.printErr(Util.getLowestMessage(t));
			return;
		}
		String filename = file.getName();
		TreeNode treeNode = new TreeNode(filename) {
			private static final long serialVersionUID = 1L;
			
			public Path getPath() {
				return config.getStorablePath(file);
			}
		};
		reporter.fail(new IndexingError(ErrorType.ENCODING, treeNode, t.getCause()));
	}
	
	// guaranteed not to be an HTML file
	protected abstract void handleFile(@NotNull File file);
	
	// if HTML pairing is off, this method will be called on HTML files as well,
	// but with empty htmlDir argument
	protected abstract void handleHtmlPair(	@NotNull File htmlFile,
											@Nullable File htmlDir);
	
	// dir will never be a symlink
	protected abstract void handleDir(@NotNull File dir);
	
	// Will be called before any of the handle methods is called
	protected abstract boolean skip(@NotNull File fileOrDir);

}
