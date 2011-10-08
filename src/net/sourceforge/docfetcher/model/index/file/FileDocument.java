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
import java.util.Map;

import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.model.DocumentType;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import de.schlichtherle.truezip.file.TFile;

@SuppressWarnings("serial")
@VisibleForPackageGroup
public final class FileDocument extends Document<FileDocument, FileFolder> {
	
	@Nullable private FileFolder htmlFolder;
	
	public FileDocument(@NotNull FileFolder parent,
	                    @NotNull String name,
						long lastModified) {
		super(parent, name, null, lastModified);
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
		Util.checkThat(newFolder == null || (newFolder instanceof TFile));
		
		if (oldFolder == null)
			return newFolder != null;
		else if (newFolder == null)
			return true;
		
		final IndexingConfig config = context.getConfig();
		final Map<String, FileDocument> unseenDocs = Maps.newHashMap(oldFolder.getDocumentMap());
		final Map<String, FileFolder> unseenSubFolders = Maps.newHashMap(oldFolder.getSubFolderMap());
		final boolean[] modificationFound = { false };
		
		new HtmlFileLister <Exception> (newFolder, config, null) {
			protected void handleFile(File file) {
				if (config.isSolidArchive(file.getName())) {
					FileFolder subFolder = unseenSubFolders.remove(file.getName());
					if (subFolder == null || !Objects.equal(subFolder.getLastModified(), file.lastModified()))
						modified();
				}
				else {
					FileDocument doc = unseenDocs.remove(file.getName());
					if (doc == null || doc.getLastModified() != file.lastModified())
						modified();
				}
			}
			protected void handleHtmlPair(File htmlFile, File htmlDir) {
				FileDocument doc = unseenDocs.remove(htmlFile.getName());
				if (doc == null || doc.isModified(context, htmlFile, htmlDir))
					modified();
			}
			protected void handleDir(File dir) {
				FileFolder subFolder = unseenSubFolders.remove(dir.getName());
				if (subFolder == null || isFolderModified(context, subFolder, dir))
					modified();
			}
			protected boolean skip(File fileOrDir) {
				return context.skip((TFile) fileOrDir);
			}
			private void modified() {
				modificationFound[0] = true;
				stop(); // Stop file lister
			}
		}.runSilently();
		
		return modificationFound[0] || !unseenDocs.isEmpty()
				|| !unseenSubFolders.isEmpty();
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
