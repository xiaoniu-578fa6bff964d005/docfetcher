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
import java.io.IOException;
import java.io.Serializable;

import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

import org.apache.lucene.store.Directory;

/**
 * This interface is used for hiding the generic parameters of {@link TreeIndex}
 * from clients.
 * 
 * @author Tran Nam Quang
 */
public interface LuceneIndex extends ViewNode, Serializable {
	
	public static final Event<LuceneIndex> evtWatchFoldersChanged = new Event<LuceneIndex>();
	
	/**
	 * Returns the directory where the Lucene index files are kept. Returns null
	 * if the Lucene index was created in memory.
	 */
	@Nullable
	public File getIndexDir();
	
	/**
	 * Returns the file or directory containing the user's files (i.e. the
	 * document repository that was indexed).
	 */
	@NotNull
	public File getRootFile();
	
	@NotNull
	public Folder<?, ?> getRootFolder();
	
	public boolean update(IndexingReporter reporter, Cancelable cancelable);
	
	@NotNull
	public Directory getLuceneDir() throws IOException;
	
	public boolean isEmailIndex();
	
	@NotNull
	public IndexingConfig getConfig();
	
	public void clear();
	
	public void delete();
	
	@NotNull
	public TreeCheckState getTreeCheckState();
	
	@NotNull
	public DocumentType getDocumentType();
	
	public boolean isWatchFolders();
	
	public void setWatchFolders(boolean watchFolders);
	
}
