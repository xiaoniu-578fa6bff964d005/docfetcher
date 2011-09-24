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

import net.sourceforge.docfetcher.model.Folder;
import net.sourceforge.docfetcher.model.FolderVisitor;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@SuppressWarnings("serial")
@VisibleForPackageGroup
public class FileFolder extends Folder<FileDocument, FileFolder> {

	public static class FileFolderVisitor <T extends Throwable>
			extends FolderVisitor<FileDocument, FileFolder, T> {
		public FileFolderVisitor(@NotNull FileFolder root) {
			super(root);
		}
		
		public FileFolderVisitor(@NotNull FileDocument doc) {
			super(doc.getHtmlFolder()); // HTML folder might be null
		}
	}
	
	public FileFolder(	@NotNull String name,
						@NotNull String path,
						@Nullable Long lastModified) {
		super(name, path, lastModified);
	}
	
	public final boolean isArchive() {
		return getLastModified() != null;
	}

}
