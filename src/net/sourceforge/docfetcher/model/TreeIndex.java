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

import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.ImmutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

@VisibleForPackageGroup
@SuppressWarnings("serial")
public abstract class TreeIndex <
	D extends Document<D, F>,
	F extends Folder<D, F>> implements LuceneIndex {
	
	public enum IndexingResult {
		SUCCESS_CHANGED,
		SUCCESS_UNCHANGED,
		FAILURE,
	}
	
	/*
	 * Note: It is important to always serialize Path objects instead of
	 * java.io.File objects to avoid certain portability issues caused by
	 * non-normalized file paths.
	 */
	
	private final IndexingConfig config;
	private final F rootFolder;
	private final long created;
	@Nullable private final Path fileIndexDirPath;
	@Nullable private transient RAMDirectory ramIndexDir;
	
	// if indexDir is null, all content is written to a RAM index, which
	// can be retrieved via getLuceneDir
	protected TreeIndex(@Nullable File indexParentDir,
	                    @NotNull File rootFile) {
		Util.checkNotNull(rootFile);
		
		// Create config
		this.config = new IndexingConfig() {
			@Override
			protected void onStoreRelativePathsChanged() {
				Path newPath = config.getStorablePath(getCanonicalRootFile());
				rootFolder.setPath(newPath);
			}
			protected void onWatchFoldersChanged() {
				LuceneIndex.evtWatchFoldersChanged.fire(TreeIndex.this);
			}
		};
		
		// Create root folder
		rootFile = Util.getCanonicalFile(rootFile);
		Path newPath = config.getStorablePath(rootFile);
		rootFolder = createRootFolder(newPath);
		Util.checkNotNull(rootFolder);
		
		// Create index directory or RAM directory
		created = Util.getTimestamp();
		if (indexParentDir == null) {
			fileIndexDirPath = null;
			ramIndexDir = new RAMDirectory();
		}
		else {
			String indexDirName = getIndexDirName(rootFile) + "_" + created;
			fileIndexDirPath = new Path(new File(indexParentDir, indexDirName).getPath());
		}
	}
	
	public final long getCreated() {
		return created;
	}
	
	@NotNull
	public final File getCanonicalRootFile() {
		return rootFolder.getPath().getCanonicalFile();
	}
	
	@NotNull
	protected abstract String getIndexDirName(@NotNull File rootFile);
	
	@NotNull
	protected abstract F createRootFolder(@NotNull Path path);

	@NotNull
	public final IndexingConfig getConfig() {
		return config;
	}
	
	@Nullable
	public final Path getIndexDirPath() {
		return fileIndexDirPath;
	}
	
	@NotNull
	public final IndexingResult update(	@Nullable IndexingReporter reporter,
										@Nullable Cancelable cancelable) {
		if (reporter == null)
			reporter = IndexingReporter.nullReporter;
		if (cancelable == null)
			cancelable = Cancelable.nullCancelable;
		if (cancelable.isCanceled())
			return IndexingResult.SUCCESS_UNCHANGED;
		return doUpdate(reporter, cancelable);
	}
	
	@NotNull
	protected abstract IndexingResult doUpdate(	@NotNull IndexingReporter reporter,
												@NotNull Cancelable cancelable);
	
	@NotNull
	public final Directory getLuceneDir() throws IOException {
		if (fileIndexDirPath != null) {
			assert ramIndexDir == null;
			return FSDirectory.open(fileIndexDirPath.getCanonicalFile());
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
		if (fileIndexDirPath != null) {
			File fileIndexDir = fileIndexDirPath.getCanonicalFile();
			if (fileIndexDir.exists()) {
				try {
					if (removeTopLevel)
						Util.deleteRecursively(fileIndexDir);
					else
						Util.deleteContents(fileIndexDir);
				}
				catch (IOException e) {
					Util.printErr(e);
				}
			}
		}
		else {
			assert ramIndexDir != null;
			ramIndexDir = new RAMDirectory();
		}
		
		/*
		 * The last-modified field of the root folder must be cleared so that
		 * the next index update will detect the root folder as modified.
		 */
		rootFolder.setLastModified(null);
		rootFolder.removeChildren();
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
	
	public final boolean hasErrorsDeep() {
		return rootFolder.hasErrorsDeep();
	}
	
}
