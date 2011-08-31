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

import java.io.CharConversionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.docfetcher.base.Stoppable;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

/**
 * @author Tran Nam Quang
 */
abstract class HtmlFileLister<T extends Throwable> extends Stoppable<T> {
	
	@NotNull private final File parentDir;
	@NotNull private final Collection<String> htmlExtensions;
	private final boolean htmlPairing;
	@Nullable private final IndexingReporter reporter;
	
	public HtmlFileLister(	@NotNull File parentDir,
							@NotNull Collection<String> htmlExtensions,
							boolean htmlPairing,
							@Nullable IndexingReporter reporter) {
		Util.checkNotNull(parentDir, htmlExtensions);
		this.parentDir = parentDir;
		this.htmlExtensions = htmlExtensions;
		this.htmlPairing = htmlPairing;
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
			if (isStopped()) return;
			if (Util.isSymLink(fileOrDir) || skip(fileOrDir))
				continue;
			if (Util.isJunctionOrSymlink(fileOrDir))
				continue;
			boolean isFile;
			try {
				isFile = fileOrDir.isFile();
			}
			catch (AssertionError e) {
				handleCharConversionException(e, fileOrDir);
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
			if (isStopped()) return;
			if (Util.isSymLink(fileOrDir))
				continue;
			if (Util.isJunctionOrSymlink(fileOrDir))
				continue;
			boolean isFile;
			try {
				isFile = fileOrDir.isFile();
			}
			catch (AssertionError e) {
				handleCharConversionException(e, fileOrDir);
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
		
		for (File dirCandidate : tempDirs) {
			if (isStopped()) return;
			String dirBasename = HtmlUtil.getHtmlDirBasename(dirCandidate);
			if (dirBasename == null) {
				if (! skip(dirCandidate))
					handleDir(dirCandidate);
				continue;
			}
			boolean htmlPairFound = false;
			for (Iterator<File> it = htmlFiles.iterator(); it.hasNext(); ) {
				File htmlCandidate = it.next();
				if (Util.splitFilename(htmlCandidate)[0].equals(dirBasename)) {
					if (! skip(htmlCandidate))
						handleHtmlPair(htmlCandidate, dirCandidate);
					it.remove();
					htmlPairFound = true;
					break;
				}
			}
			if (! htmlPairFound && ! skip(dirCandidate))
				handleDir(dirCandidate);
		}
		
		// Visit unpaired html files
		for (File htmlFile : htmlFiles) {
			if (isStopped()) return;
			if (! skip(htmlFile))
				handleHtmlPair(htmlFile, null);
		}
	}

	private boolean isHtmlFile(@NotNull File file) {
		return Util.hasExtension(file.getName(), htmlExtensions);
	}
	
	private void handleCharConversionException(	@NotNull AssertionError e,
												@NotNull final File file) {
		/*
		 * TrueZIP crashes with a CharConversionException while traversing
		 * certain zip files containing Chinese encodings.
		 */
		if (!(e.getCause() instanceof CharConversionException))
			throw e;
		if (reporter == null) {
			Util.printErr(e.getCause().getMessage());
			return;
		}
		String filename = file.getName();
		@SuppressWarnings("serial")
		TreeNode treeNode = new TreeNode(filename, filename) {
			public String getPath() {
				return file.getPath();
			}
		};
		reporter.fail(new IndexingError(ErrorType.ENCODING, treeNode, e.getCause()));
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
