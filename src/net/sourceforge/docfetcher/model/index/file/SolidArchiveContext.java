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

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.model.index.file.SolidArchiveTree.FailReporter;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import de.schlichtherle.truezip.file.TArchiveDetector;

/**
 * @author Tran Nam Quang
 */
final class SolidArchiveContext extends FileContext implements FailReporter {
	
	private final boolean isTempArchive; // Whether to delete the archive or not
	
	/*
	 * Maps from documents and nested archives to their parents. We'll need
	 * those parents when we have to detach some documents and nested archives
	 * from the tree.
	 */
	final Map<FileDocument, FileFolder> addedDocs = new HashMap<FileDocument, FileFolder> ();
	final Map<FileDocument, FileFolder> modifiedDocs = new HashMap<FileDocument, FileFolder> ();
	final Map<FileFolder, FileFolder> nestedArchives = new HashMap<FileFolder, FileFolder> ();

	protected SolidArchiveContext(	@NotNull IndexingConfig config,
	                              	@NotNull TArchiveDetector zipDetector,
	                              	@NotNull LuceneDocWriter writer,
	                              	@NotNull IndexingReporter reporter,
	                              	@Nullable String originalPath,
	                              	@NotNull Cancelable cancelable,
	                              	boolean isTempArchive) {
		super(config, zipDetector, writer, reporter, originalPath, cancelable);
		this.isTempArchive = isTempArchive;
	}
	
	protected SolidArchiveContext(	@NotNull FileContext superContext,
									@NotNull String originalPath,
									boolean isTempArchive) {
		this(
				superContext.getConfig(),
				superContext.getZipDetector(),
				superContext.getWriter(),
				superContext.getReporter(),
				originalPath,
				superContext.getStopper(),
				isTempArchive
		);
	}
	
	public IndexingConfig getIndexingConfig() {
		return getConfig();
	}
	
	public boolean hasEntriesToUnpack() {
		return addedDocs.size() + modifiedDocs.size() + nestedArchives.size() > 0;
	}
	
	public boolean isTempArchive() {
		return isTempArchive;
	}
	
}