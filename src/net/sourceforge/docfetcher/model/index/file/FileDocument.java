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
import java.io.FileFilter;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.model.DocumentType;
import de.schlichtherle.truezip.file.TFile;

final class FileDocument extends Document<FileDocument, FileFolder> {
	
	private static final long serialVersionUID = 6387439091828861893L;
	
	@Nullable private FileFolder htmlFolder;
	
	public FileDocument(@NotNull FileFolder parent,
	                    @NotNull String name,
						long lastModified) {
		super(parent, name, name, lastModified);
	}
	
	@NotNull
	protected final DocumentType getType() {
		return DocumentType.FILE;
	}

	@Nullable
	public FileFolder getHtmlFolder() {
		return htmlFolder;
	}
	
	public void setHtmlFolder(@Nullable FileFolder htmlFolder) {
		this.htmlFolder = htmlFolder;
	}
	
	public boolean isModified(	@NotNull FileContext context,
	                          	@NotNull File file,
								@Nullable File htmlFolder) {
		Util.checkThat(getName().equals(file.getName()));
		if (getLastModified() != file.lastModified())
			return true;
		return isFolderModified(context, this.htmlFolder, htmlFolder);
	}
	
	@RecursiveMethod
	private static boolean isFolderModified(@NotNull final FileContext context,
											@Nullable FileFolder oldFolder,
											@Nullable File newFolder) {
		if (oldFolder == null)
			return newFolder != null;
		else if (newFolder == null)
			return true;

		File[] files = Util.listFiles(newFolder, new FileFilter() {
			public boolean accept(File file) {
				return !context.skip((TFile) file);
			}
		});
		
		if (files.length != oldFolder.getChildCount())
			return true;

		for (File fileOrDir : files) {
			String name = fileOrDir.getName();
			if (fileOrDir.isFile()) {
				FileDocument doc = oldFolder.getDocument(name);
				if (doc == null)
					return true;
				if (doc.getLastModified() != fileOrDir.lastModified())
					return true;
			}
			else if (fileOrDir.isDirectory()) {
				FileFolder subFolder = oldFolder.getSubFolder(name);
				if (subFolder == null)
					return true;
				if (isFolderModified(context, subFolder, fileOrDir))
					return true;
			}
		}

		return false;
	}
	
	// new doc must have the same name
	public boolean isModified(@NotNull FileDocument newDoc) {
		Util.checkThat(getName().equals(newDoc.getName()));
		if (getLastModified() != newDoc.getLastModified())
			return true;
		return isFolderModified(htmlFolder, newDoc.htmlFolder);
	}
	
	@RecursiveMethod
	private static boolean isFolderModified(@Nullable FileFolder oldFolder,
											@Nullable FileFolder newFolder) {
		if (oldFolder == null)
			return newFolder != null;
		else if (newFolder == null)
			return true;
		
		if (oldFolder.getDocumentCount() != newFolder.getDocumentCount())
			return true;
		
		if (oldFolder.getSubFolderCount() != newFolder.getSubFolderCount())
			return true;
		
		for (FileDocument oldDoc : oldFolder.getDocuments()) {
			FileDocument newDoc = newFolder.getDocument(oldDoc.getName());
			if (newDoc == null || oldDoc.getLastModified() != newDoc.getLastModified())
				return true;
		}
		
		for (FileFolder oldSubFolder : oldFolder.getSubFolders()) {
			FileFolder newSubFolder = newFolder.getSubFolder(oldSubFolder.getName());
			if (newSubFolder == null || isFolderModified(oldSubFolder, newSubFolder))
				return true;
		}
		
		return false;
	}
	
}
