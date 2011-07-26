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

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.model.Folder;
import net.sourceforge.docfetcher.model.FolderVisitor;

/**
 * @author Tran Nam Quang
 */
class FileFolder extends Folder<FileDocument, FileFolder> {

	private static final long serialVersionUID = -901982674932899773L;

	public static class FileFolderVisitor <T extends Throwable>
			extends FolderVisitor<FileDocument, FileFolder, T> {
		public FileFolderVisitor(@NotNull FileFolder root) {
			super(root);
		}
	}
	
	/**
	 * The last time this object was modified. A null value indicates this is a
	 * regular folder while a non-null value represents an archive.
	 */
	@Nullable private Long lastModified;

	public FileFolder(	@NotNull String name,
						@NotNull String path,
						@Nullable Long lastModified) {
		super(name, path);
		this.lastModified = lastModified;
	}

	@Nullable
	public final Long getLastModified() {
		return lastModified;
	}
	
	public final boolean isArchive() {
		return lastModified != null;
	}
	
	public final void setLastModified(@Nullable Long lastModified) {
		this.lastModified = lastModified;
	}

}
