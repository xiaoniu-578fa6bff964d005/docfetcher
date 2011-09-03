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

package net.sourceforge.docfetcher.model;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.ImmutableCopy;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;

@VisibleForPackageGroup
@SuppressWarnings("serial")
public abstract class TreeIndex <
	D extends Document<D, F>,
	F extends Folder<D, F>,
	C extends IndexingConfig> implements LuceneIndex {
	
	@Nullable private final File fileIndexDir;
	@Nullable private transient RAMDirectory ramIndexDir;
	@NotNull private final F rootFolder;
	@NotNull private C config;
	
	// if indexDir is null, all content is written to a RAM index, which
	// can be retrieved via getLuceneDir
	protected TreeIndex(@NotNull C config,
						@Nullable File indexDir,
						@NotNull F rootFolder) {
		Util.checkNotNull(config, rootFolder);
		this.config = config;
		this.fileIndexDir = indexDir;
		this.rootFolder = rootFolder;
		if (indexDir == null)
			ramIndexDir = new RAMDirectory();
	}
	
	// returns success
	public abstract boolean update(	@NotNull IndexingReporter reporter,
									@NotNull Cancelable cancelable);

	@NotNull
	public final C getConfig() {
		return config;
	}
	
	@NotNull
	public final File getRootFile() {
		return new File(rootFolder.getPath());
	}
	
	@Nullable
	public final File getIndexDir() {
		return fileIndexDir;
	}
	
	@NotNull
	@VisibleForTesting
	public final Directory getLuceneDir() throws IOException {
		if (fileIndexDir != null) {
			assert ramIndexDir == null;
			return FSDirectory.open(fileIndexDir);
		}
		if (ramIndexDir == null) // may be null after deserialization
			ramIndexDir = new RAMDirectory();
		return ramIndexDir;
	}
	
	@NotNull
	public final F getRootFolder() {
		return rootFolder;
	}
	
	@NotNull
	public final String getDisplayName() {
		return rootFolder.getDisplayName();
	}
	
	@NotNull
	public final Iterable<ViewNode> getChildren() {
		return rootFolder.getChildren();
	}
	
	public final void clear() {
		clear(false);
	}
	
	public final void delete() {
		clear(true);
	}
	
	private void clear(boolean removeTopLevel) {
		rootFolder.removeChildren();
		if (fileIndexDir != null) {
			try {
				if (removeTopLevel)
					Files.deleteRecursively(fileIndexDir);
				else
					Files.deleteDirectoryContents(fileIndexDir);
			}
			catch (IOException e) {
				Util.printErr(e);
			}
		}
		else {
			assert ramIndexDir != null;
			ramIndexDir = new RAMDirectory();
		}
	}
	
	public final boolean isChecked() {
		return rootFolder.isChecked();
	}
	
	public final void setChecked(boolean isChecked) {
		rootFolder.setChecked(isChecked);
	}
	
	@NotNull
	public final TreeCheckState getTreeCheckState() {
		return rootFolder.getTreeCheckState();
	}
	
	public final boolean isIndex() {
		return true;
	}
	
	@ImmutableCopy
	@NotNull
	public final List<String> getDocumentIds() {
		return rootFolder.getDocumentIds();
	}
	
	public final boolean isWatchFolders() {
		return config.isWatchFolders();
	}
	
	public final void setWatchFolders(boolean watchFolders) {
		if (config.isWatchFolders() == watchFolders)
			return;
		config.setWatchFolders(watchFolders);
		LuceneIndex.evtWatchFoldersChanged.fire(this);
	}
	
}
