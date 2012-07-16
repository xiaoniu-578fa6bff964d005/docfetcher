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

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingInfo;
import net.sourceforge.docfetcher.model.index.IndexingInfo.InfoType;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.model.index.MutableInt;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.model.parse.ParseResult;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;

/**
 * @author Tran Nam Quang
 */
class FileContext {
	
	private final IndexingConfig config;
	private final TArchiveDetector zipDetector;
	private final LuceneDocWriter writer;
	@NotNull private IndexingReporter reporter;
	@Nullable private final Path originalPath;
	private final Cancelable cancelable;
	private final MutableInt fileCount;
	@Nullable private final File indexParentDir; // null if index only exists in RAM

	protected FileContext(	@NotNull IndexingConfig config,
							@NotNull TArchiveDetector zipDetector,
							@NotNull LuceneDocWriter writer,
							@Nullable IndexingReporter reporter,
							@Nullable Path originalPath,
							@NotNull Cancelable cancelable,
							@NotNull MutableInt fileCount,
							@Nullable File indexParentDir) {
		Util.checkNotNull(config, zipDetector, writer, cancelable, fileCount);
		this.config = config;
		this.zipDetector = zipDetector;
		this.writer = writer;
		this.originalPath = originalPath;
		this.cancelable = cancelable;
		this.fileCount = fileCount;
		this.indexParentDir = indexParentDir;
		setReporter(reporter);
	}
	
	protected FileContext(	@NotNull FileContext superContext,
							@NotNull Path originalPath) {
		this(
				superContext.config,
				superContext.zipDetector,
				superContext.writer,
				superContext.reporter,
				originalPath,
				superContext.cancelable,
				superContext.fileCount,
				superContext.indexParentDir
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
	protected final IndexingReporter getReporter() {
		return reporter;
	}

	public final void setReporter(@Nullable IndexingReporter reporter) {
		this.reporter = reporter == null
			? IndexingReporter.nullReporter
			: reporter;
	}
	
	@Nullable
	public final Path getOriginalPath() {
		return originalPath;
	}
	
	public final boolean isStopped() {
		return cancelable.isCanceled();
	}
	
	@NotNull
	protected final Cancelable getStopper() {
		return cancelable;
	}
	
	@NotNull
	protected final MutableInt getFileCount() {
		return fileCount;
	}
	
	@Nullable
	protected final File getIndexParentDir() {
		return indexParentDir;
	}
	
	// returns success
	// if the indexing is canceled before or during the execution of this method,
	// the last-modified value of the given document will be set to -1.
	public final boolean index(	@NotNull FileDocument doc,
								@NotNull File file,
								boolean isAdded) throws IndexingException {
		info(InfoType.EXTRACTING, doc);
		try {
			// Text extraction; may throw OutOfMemoryErrors
			ParseResult parseResult = ParseService.parse(
				config, file, doc.getName(), doc.getPath(), reporter, cancelable);
			
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
			
			// Clear errors from previous indexing operations
			doc.setError(null);
			
			return true;
		}
		catch (IOException e) {
			throw new IndexingException(e);
		}
		catch (ParseException e) {
			fail(ErrorType.PARSING, doc, e);
		}
		catch (CheckedOutOfMemoryError e) {
			fail(ErrorType.OUT_OF_MEMORY, doc, e.getCause());
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
	
	public void info(@NotNull InfoType type, @NotNull TreeNode treeNode) {
		fileCount.increment();
		reporter.info(new IndexingInfo(type, treeNode, fileCount.get()));
	}
	
	// Reports the given error and saves it in the given tree node
	public final void fail(	@NotNull ErrorType type,
							@NotNull TreeNode treeNode,
							@Nullable Throwable cause) {
		UtilModel.fail(reporter, type, treeNode, cause);
	}
	
	/**
	 * Returns the original path for the given file, taking into account virtual
	 * files inside TrueZIP archives. The returned path depends on whether a
	 * non-null original path was set on the receiver.
	 */
	@NotNull
	public final Path getDirOrZipPath(@NotNull File file) {
		if (originalPath == null)
			return config.getStorablePath(file);
		TFile tzFile = (TFile) file;
		TFile topArchive = tzFile.getTopLevelArchive();
		String relativePath = UtilModel.getRelativePath(topArchive, tzFile);
		return originalPath.createSubPath(relativePath);
	}
	
	/**
	 * Returns whether the given TrueZIP file or directory should be skipped,
	 * given the various settings of the receiver.
	 */
	public final boolean skip(@NotNull TFile fileOrDir) {
		String filename = fileOrDir.getName();
		Path filepath = getDirOrZipPath(fileOrDir);
		
		boolean isFileOrSolidArchive = fileOrDir.isFile();
		boolean isZipArchiveOrFolder = !isFileOrSolidArchive;
		boolean isZipArchive = isZipArchiveOrFolder
			? UtilModel.isZipArchive(fileOrDir)
			: false;
		boolean isFileOrArchive = isFileOrSolidArchive || isZipArchive;
		boolean isFile = isFileOrSolidArchive
				&& !config.isSolidArchive(filename);
		
		for (PatternAction patternAction : config.getPatternActions()) {
			switch (patternAction.getAction()) {
			case EXCLUDE:
				if (patternAction.matches(filename, filepath, isFileOrArchive))
					return true;
				break;
			case DETECT_MIME:
				/*
				 * If the mime pattern matches, we'll check the mime pattern
				 * again later (right before parsing) in order to determine
				 * whether to detect the filetype by filename or by mimetype.
				 */
				if (isFile && patternAction.matches(filename, filepath, isFile))
					return false;
				break;
			default:
				throw new IllegalStateException();
			}
		}
		return isFile && !ParseService.canParseByName(config, filename);
	}

}