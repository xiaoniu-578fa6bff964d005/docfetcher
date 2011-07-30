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
import java.io.IOException;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.model.index.IndexingReporter.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingReporter.InfoType;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.model.parse.ParseResult;
import net.sourceforge.docfetcher.model.parse.ParseService;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;

/**
 * @author Tran Nam Quang
 */
class FileContext {
	
	private static final IndexingReporter nullReporter = new IndexingReporter();
	
	@NotNull private final IndexingConfig config;
	@NotNull private final TArchiveDetector zipDetector;
	@NotNull private LuceneDocWriter writer;
	@NotNull private IndexingReporter reporter;
	@Nullable private final String originalPath;
	@NotNull private final Cancelable cancelable;

	protected FileContext(	@NotNull IndexingConfig config,
							@NotNull TArchiveDetector zipDetector,
							@NotNull LuceneDocWriter writer,
							@Nullable IndexingReporter reporter,
							@Nullable String originalPath,
							@NotNull Cancelable cancelable) {
		Util.checkNotNull(config, zipDetector, writer, cancelable);
		this.config = config;
		this.zipDetector = zipDetector;
		this.writer = writer;
		this.originalPath = originalPath;
		this.cancelable = cancelable;
		setReporter(reporter);
	}
	
	protected FileContext(	@NotNull FileContext superContext,
							@NotNull String originalPath) {
		this(
				superContext.config,
				superContext.zipDetector,
				superContext.writer,
				superContext.reporter,
				originalPath,
				superContext.cancelable
		);
	}
	
	@NotNull
	public final IndexingConfig getConfig() {
		return config;
	}
	
	@NotNull
	public final TArchiveDetector getZipDetector() {
		return zipDetector;
	}
	
	@NotNull
	public final LuceneDocWriter getWriter() {
		return writer;
	}
	
	// May return a default empty reporter
	@NotNull
	public final IndexingReporter getReporter() {
		return reporter;
	}

	public final void setReporter(@Nullable IndexingReporter reporter) {
		this.reporter = reporter == null ? nullReporter : reporter;
	}
	
	@Nullable
	public final String getOriginalPath() {
		return originalPath;
	}
	
	public final boolean isStopped() {
		return cancelable.isCanceled();
	}
	
	protected final Cancelable getStopper() {
		return cancelable;
	}
	
	// returns success
	// if the indexing is canceled before or during the execution of this method,
	// the last-modified value of the given document will be set to -1.
	public final boolean index(	@NotNull FileDocument doc,
								@NotNull File file,
								boolean isAdded) throws IndexingException {
		reporter.info(InfoType.EXTRACTING, doc);
		try {
			// Text extraction; may throw OutOfMemoryErrors
			ParseResult parseResult = ParseService.parse(
				config, doc.getName(), file, cancelable);
			
			/*
			 * If we detect a cancel request at this point, the request probably
			 * came in during the parsing step above. In that case, we'll keep
			 * the partially extracted text and feed it to Lucene, but set the
			 * stored last-modified value to -1 so that the next index update
			 * will see the file as "modified" and therefore reindex it.
			 */
			if (cancelable.isCanceled())
				doc.setLastModified(-1);

			// Add to index or update in index; may also throw OutOfMemoryErrors
			if (isAdded)
				writer.add(doc, file, parseResult);
			else
				writer.update(doc, file, parseResult);
			return true;
		}
		catch (IOException e) {
			throw new IndexingException(e);
		}
		catch (ParseException e) {
			reporter.fail(ErrorType.PARSING, doc, e);
		}
		catch (OutOfMemoryError e) {
			reporter.fail(ErrorType.OUT_OF_MEMORY, doc, e);
		}
		catch (StackOverflowError e) {
			reporter.fail(ErrorType.STACK_OVERFLOW, doc, e);
		}
		return false;
	}
	
	public final boolean indexAndDeleteFile(@NotNull FileDocument doc,
											@NotNull File file,
											boolean added)
			throws IndexingException {
		try {
			return index(doc, file, added);
		}
		finally {
			file.delete();
		}
	}
	
	public final void deleteFromIndex(@NotNull String uid)
			throws IndexingException {
		try {
			writer.delete(uid);
		}
		catch (IOException e) {
			throw new IndexingException(e);
		}
	}
	
	public final void info(InfoType type, TreeNode treeNode) {
		reporter.info(type, treeNode);
	}
	
	public final void fail(	ErrorType type,
							TreeNode treeNode,
							@Nullable Throwable cause) {
		reporter.fail(type, treeNode, cause);
	}
	
	/**
	 * Returns the original path for the given file, taking into account virtual
	 * files inside TrueZIP archives. The returned path depends on whether a
	 * non-null original path for the given indexing context was set.
	 */
	@NotNull
	public final String getDirOrZipPath(@NotNull File file) {
		if (originalPath == null)
			return config.getStorablePath(file);
		TFile tzFile = (TFile) file;
		TFile topArchive = tzFile.getTopLevelArchive();
		String relativePath = UtilModel.getRelativePath(topArchive, tzFile);
		return Util.joinPath(originalPath, relativePath);
	}
	
	/**
	 * Returns whether the given TrueZIP file or directory should be skipped,
	 * given the various settings of the receiver.
	 */
	public final boolean skip(@NotNull TFile fileOrDir) {
		String filename = fileOrDir.getName();
		String filepath = getDirOrZipPath(fileOrDir);
		boolean isFileOrSolidArchive = fileOrDir.isFile();
		boolean isZipArchiveOrFolder = !isFileOrSolidArchive;
		boolean isZipArchive = isZipArchiveOrFolder
			? UtilModel.isZipArchive(fileOrDir)
			: false;
		boolean isFileOrArchive = isFileOrSolidArchive || isZipArchive;
		if (config.getFileFilter().matches(
			filename, filepath, isFileOrArchive)) return true;
		/*
		 * If the mime pattern matches, we'll check the mime pattern again later
		 * (right before parsing) in order to determine whether to detect the
		 * filetype by filename or by mimetype.
		 */
		boolean isFile = isFileOrSolidArchive
				&& !config.isSolidArchive(filename);
		if (isFile && !config.matchesMimePattern(filename))
			return !ParseService.canParseByName(config, filename);
		return false;
	}

}