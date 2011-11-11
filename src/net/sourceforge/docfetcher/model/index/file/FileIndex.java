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
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.DocumentType;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.TreeIndex;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.DiskSpaceException;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingInfo.InfoType;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.model.index.MutableInt;
import net.sourceforge.docfetcher.model.index.file.FileFolder.FileFolderVisitor;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsSyncException;

/**
 * @author Tran Nam Quang
 */
@SuppressWarnings("serial")
public final class FileIndex extends TreeIndex<FileDocument, FileFolder> {

	/*
	 * General exception handling policy used here:
	 * 
	 * (1) IOExceptions from read/write operations on the Lucene index are
	 * encapsulated as IndexingExceptions and propagated upwards.
	 * 
	 * (2) IOExceptions from user files are reported and swallowed locally.
	 * TrueZIP is used instead of java.util.zip because it makes HTML pairing
	 * easier, and putting java.util.zip into solid archive tree will unpack all
	 * files in one pass, which isn't necessary. Generally, a document or folder
	 * is stored in the tree even if the parsing fails.
	 * 
	 * (3) A parser failure is treated as if we had successfully extracted an
	 * empty string. The point of this is to avoid parsing and failing on the
	 * same problematic files over and over again on each subsequent index
	 * update: If we failed the first time and the file hasn't changed in the
	 * meantime, there's no point in trying to parse it again because we know
	 * we'd fail.
	 */

	// if indexParentDir is null, all content is written to a RAM index, which
	// can be retrieved via getLuceneDir
	public FileIndex(@Nullable File indexParentDir, @NotNull File rootFile) {
		super(indexParentDir, rootFile);
		
		/*
		 * If the given file object is a file, adjust the indexing configuration
		 * as needed.
		 */
		if (rootFile.isFile()) {
			IndexingConfig config = getConfig();
			String extension = Util.getExtension(rootFile);
			if (extension.equals("exe")) {
				config.setDetectExecutableArchives(true);
			}
			else if (!config.isSolidArchive(rootFile.getName())
					&& !IndexingConfig.hiddenZipExtensions.contains(extension)) {
				List<String> zipExtensions = config.getZipExtensions();
				if (!zipExtensions.contains(extension)) {
					List<String> newZipExtensions = Util.createList(
						zipExtensions, extension);
					config.setZipExtensions(newZipExtensions);
				}
			}
		}
	}
	
	@NotNull
	protected String getIndexDirName(@NotNull File rootFile) {
		return Util.getNameOrLetter(rootFile, "");
	}

	@NotNull
	protected FileFolder createRootFolder(@NotNull Path path) {
		return new FileFolder(path, null);
	}

	public boolean isEmailIndex() {
		return false;
	}
	
	public DocumentType getDocumentType() {
		return DocumentType.FILE;
	}

	public IndexingResult doUpdate(	@NotNull IndexingReporter reporter,
									@NotNull Cancelable cancelable) {
		reporter.setStartTime(System.currentTimeMillis());
		IndexingConfig config = getConfig();
		FileFolder rootFolder = getRootFolder();
		rootFolder.setError(null);
		SimpleDocWriter writer = null;

		/*
		 * Wrap the stored root file in a TFile to enable zip archive support.
		 * 
		 * Note that both the path of the rootFolder object and the path of the
		 * stored root file will be empty if the root corresponds to the current
		 * working directory. This is why the TFile is initialized with an
		 * absolute file here: Otherwise TrueZIP would fail to recognize that
		 * it's an existing directory.
		 */
		TArchiveDetector zipDetector = config.createZipDetector();
		TFile rootFile = new TFile(getCanonicalRootFile(), zipDetector);

		try {
			/*
			 * The user-defined zip extensions have higher priority, so we'll
			 * check for folders and zip archives first.
			 */
			if (rootFile.isDirectory()) {
				/*
				 * Return immediately if the root file is a zip archive and it
				 * wasn't modified.
				 */
				Long newLastModified = getZipArchiveLastModified(config, rootFile);
				if (UtilModel.isUnmodifiedArchive(rootFolder, newLastModified))
					return IndexingResult.SUCCESS_UNCHANGED;
				rootFolder.setLastModified(newLastModified);
				
				writer = new SimpleDocWriter(getLuceneDir());
				FileContext context = new FileContext(
					config, zipDetector, writer, reporter, null, cancelable,
					new MutableInt(0));
				visitDirOrZip(context, rootFolder, rootFile);
			}
			else {
				// Return immediately if the root file wasn't modified
				long newLastModified = rootFile.lastModified();
				if (UtilModel.isUnmodifiedArchive(rootFolder, newLastModified))
					return IndexingResult.SUCCESS_UNCHANGED;
				rootFolder.setLastModified(newLastModified);
				
				SolidArchiveFactory factory = config
						.getSolidArchiveFactory(rootFile.getName());
				if (factory == null) {
					/*
					 * This happens when the user tries to index a file that
					 * isn't an archive.
					 * 
					 * TODO i18n: target may be an archive whose format isn't
					 * supported
					 */
					report(ErrorType.NOT_AN_ARCHIVE, reporter, null);
					return IndexingResult.FAILURE;
				}
				
				writer = new SimpleDocWriter(getLuceneDir());
				SolidArchiveContext context = new SolidArchiveContext(
					config, zipDetector, writer, reporter, null, cancelable,
					new MutableInt(0), false);
				SolidArchiveTree<?> archiveTree = factory.createSolidArchiveTree(
					context, rootFile);
				visitSolidArchive(context, rootFolder, archiveTree);
			}

			writer.optimize();
			return IndexingResult.SUCCESS_CHANGED;
		}
		catch (ArchiveEncryptedException e) {
			report(ErrorType.ARCHIVE_ENCRYPTED, reporter, e);
		}
		catch (IOException e) {
			ErrorType errorType = Util.hasExtension(rootFolder.getName(), "exe")
				? ErrorType.NOT_AN_ARCHIVE
				: ErrorType.IO_EXCEPTION;
			report(errorType, reporter, e);
		}
		catch (IndexingException e) {
			report(ErrorType.IO_EXCEPTION, reporter, e.getIOException());
		}
		finally {
			Closeables.closeQuietly(writer);
			reporter.setEndTime(System.currentTimeMillis());
		}
		return IndexingResult.FAILURE;
	}
	
	private void report(@NotNull ErrorType errorType,
	                    @NotNull IndexingReporter reporter,
						@Nullable Exception e) {
		FileFolder rootFolder = getRootFolder();
		IndexingError error = new IndexingError(errorType, rootFolder, e);
		rootFolder.setError(error);
		reporter.fail(error);
	}

	/**
	 * Returns the last-modified attribute of the given zip archive, or null if
	 * the given file is not a zip archive. This method does not work for files
	 * inside archive files: In the latter case, it always returns null.
	 */
	@Nullable
	private static Long getZipArchiveLastModified(	@NotNull IndexingConfig config,
													@NotNull File file) {
		if (file instanceof TFile) {
			if (UtilModel.isZipArchive((TFile) file))
				return file.lastModified();
		}
		else if (file.isFile() && config.isArchive(file.getName())) {
			/*
			 * Normally, we shouldn't end up in this else-branch because all
			 * incoming files are expected to be TrueZIP files.
			 */
			return file.lastModified();
		}
		return null;
	}

	@NotNull
	private static FileDocument createFileDoc(	@NotNull FileFolder parentFolder,
												@NotNull File file) {
		return new FileDocument(parentFolder, file.getName(), file.lastModified());
	}

	// Will clean up temporary zip files
	@RecursiveMethod
	private static void visitDirOrZip(	@NotNull final FileContext context,
										@NotNull final FileFolder folder,
										@NotNull final File dirOrZip)
			throws IndexingException {
		assert dirOrZip.isDirectory();
		assert !folder.hasErrors();
		
		final Map<String, FileDocument> unseenDocs = Maps.newHashMap(folder.getDocumentMap());
		final Map<String, FileFolder> unseenSubFolders = Maps.newHashMap(folder.getSubFolderMap());

		/*
		 * Note: If the user aborts the indexing, the file tree must be left in
		 * a consistent state, so that the user can continue indexing later.
		 */
		new HtmlFileLister<IndexingException>(
			dirOrZip, context.getConfig(), context.getReporter()) {
			protected void handleFile(@NotNull File file) {
				if (context.isStopped()) stop();
				try {
					if (switchDirZipToSolid(context, folder, file)) {
						unseenSubFolders.remove(file.getName());
						return;
					}
					FileDocument doc = unseenDocs.remove(file.getName());
					// File added
					if (doc == null) {
						doc = createFileDoc(folder, file);
						context.index(doc, file, true);
					}
					// File modified
					else if (doc.isModified(context, file, null)) {
						doc.setLastModified(file.lastModified());
						doc.setHtmlFolder(null);
						
						/*
						 * Try to index the file. If this fails, remove it from
						 * the Lucene index, but keep it in the tree so we won't
						 * index it again on the next index update.
						 */
						if (!context.index(doc, file, false))
							context.deleteFromIndex(doc.getUniqueId());
					}
				}
				catch (IndexingException e) {
					stop(e);
				}
			}

			protected void handleHtmlPair(	@NotNull File htmlFile,
											@Nullable File htmlDir) {
				if (context.isStopped()) stop();
				try {
					FileDocument doc = unseenDocs.remove(htmlFile.getName());
					// HTML pair added
					if (doc == null) {
						doc = createFileDoc(folder, htmlFile);
						FileFolder htmlFolder = htmlDir == null
							? null
							: new FileFolder(
								context.getDirOrZipPath(htmlDir), null);
						doc.setHtmlFolder(htmlFolder);
						AppendingContext subContext = new AppendingContext(
							context);
						if (!subContext.index(doc, htmlFile, true)) return;
						if (htmlDir != null) {
							subContext.setReporter(null);
							visitDirOrZip(
								subContext, doc.getHtmlFolder(), htmlDir);
						}
						subContext.appendToOuter(doc, true);
					}
					// HTML pair modified
					else if (doc.isModified(context, htmlFile, htmlDir)) {
						doc.setLastModified(htmlFile.lastModified());
						/*
						 * Here, we replace any previous HTML folder with a new
						 * empty one, which effectively causes all files
						 * encountered in the on-disk HTML folder to appear as
						 * 'new', rather than 'modified' or 'removed'.
						 */
						FileFolder htmlFolder = htmlDir == null
							? null
							: new FileFolder(
								context.getDirOrZipPath(htmlDir), null);
						doc.setHtmlFolder(htmlFolder);
						AppendingContext subContext = new AppendingContext(
							context);
						if (subContext.index(doc, htmlFile, true)) {
							if (htmlDir != null) {
								subContext.setReporter(null);
								visitDirOrZip(
									subContext, doc.getHtmlFolder(), htmlDir);
							}
							subContext.appendToOuter(doc, false);
						}
						else {
							context.deleteFromIndex(doc.getUniqueId());
						}
					}
				}
				catch (IndexingException e) {
					stop(e);
				}
			}

			protected void handleDir(@NotNull File dir) {
				if (context.isStopped()) stop();
				/*
				 * The Folder object's last-modified attribute is non-null for
				 * zip archives. This allows us to avoid recursion into
				 * unmodified zip archives.
				 */
				FileFolder subFolder = unseenSubFolders.remove(dir.getName());
				Long newLastModified = getZipArchiveLastModified(
					context.getConfig(), dir);
				if (subFolder == null) { // Folder added
					subFolder = new FileFolder(folder, dir.getName(), newLastModified);
				}
				else { // Folder already registered, check modification state
					if (UtilModel.isUnmodifiedArchive(subFolder, newLastModified))
						return;
					subFolder.setLastModified(newLastModified);
					subFolder.setError(null);
				}
				try {
					visitDirOrZip(context, subFolder, dir);
				}
				catch (IndexingException e) {
					stop(e);
				}
			}

			protected boolean skip(@NotNull File fileOrDir) {
				return context.skip((TFile) fileOrDir);
			}

			protected void runFinally() {
				// Delete temporary zip files
				try {
					if (!(dirOrZip instanceof TFile)) return;
					TFile tzFile = (TFile) dirOrZip;
					// Without the following if-clause TrueZIP would throw an
					// exception
					if (tzFile.isArchive() && tzFile.getEnclArchive() == null)
						TFile.umount(tzFile);
				}
				catch (FsSyncException e) {
					stop(new IndexingException(e));
				}
			}
		}.run();

		if (context.isStopped()) return;

		// Handle missing files and folders
		for (FileDocument doc : unseenDocs.values()) {
			/*
			 * Note: Deleting the document from the Lucene index requires
			 * constructing the document's UID using the parent folder's path,
			 * so we must do this before detaching the document from the parent
			 * folder.
			 */
			context.deleteFromIndex(doc.getUniqueId());
			folder.removeDocument(doc);
		}
		for (FileFolder subFolder : unseenSubFolders.values())
			detachMissingSubFolder(context, folder, subFolder);
	}
	
	private static void detachMissingSubFolder(	@NotNull final FileContext context,
												@NotNull final FileFolder parent,
												@NotNull FileFolder missingFolder)
			throws IndexingException {
		parent.removeSubFolder(missingFolder);
		new FileFolderVisitor<IndexingException>(missingFolder) {
			public void visitDocument(	FileFolder parent,
			                          	FileDocument fileDocument) {
				try {
					context.deleteFromIndex(fileDocument.getUniqueId());
				}
				catch (IndexingException e) {
					stop(e); // stop visitor
				}
			}
		}.run();
	}

	// Returns true if the caller can skip processing the given archive file
	private static boolean switchDirZipToSolid(	@NotNull FileContext context,
												@NotNull FileFolder parentFolder,
												@NotNull File archiveFile)
			throws IndexingException {
		String archiveName = archiveFile.getName();
		SolidArchiveFactory factory = context.getConfig()
				.getSolidArchiveFactory(archiveName);
		if (factory == null) return false;

		// Create or get subfolder object
		FileFolder archiveFolder = parentFolder.getSubFolder(archiveName);
		long newLastModified = archiveFile.lastModified();
		if (archiveFolder == null) { // Found new archive
			archiveFolder = new FileFolder(
				parentFolder, archiveName, newLastModified);
		}
		else { // Found registered archive
			if (UtilModel.isUnmodifiedArchive(archiveFolder, newLastModified))
				return true; // Don't recurse into unmodified archive
			archiveFolder.setLastModified(newLastModified);
			archiveFolder.setError(null);
		}

		File unpackedArchiveFile = null;
		try {
			unpackedArchiveFile = UtilModel.maybeUnpackZipEntry(
				context.getConfig(), archiveFile);
			boolean isTempArchive = unpackedArchiveFile != null;
			SolidArchiveContext subContext = new SolidArchiveContext(
				context, archiveFolder.getPath(), isTempArchive);
			SolidArchiveTree<?> archiveTree = factory.createSolidArchiveTree(
				subContext, isTempArchive ? unpackedArchiveFile : archiveFile);
			visitSolidArchive(subContext, archiveFolder, archiveTree);
		}
		catch (DiskSpaceException e) {
			archiveFolder.removeChildren();
			context.fail(ErrorType.ARCHIVE_UNPACK_DISKSPACE, archiveFolder, e);
		}
		catch (IOException e) {
			archiveFolder.removeChildren();
			ErrorType errorType = Util.hasExtension(archiveName, "exe")
				? ErrorType.NOT_AN_ARCHIVE
				: ErrorType.ARCHIVE;
			context.fail(errorType, archiveFolder, e);
		}
		catch (ArchiveEncryptedException e) {
			archiveFolder.removeChildren();
			context.fail(ErrorType.ARCHIVE_ENCRYPTED, archiveFolder, e);
		}

		return true;
	}

	// will close the archive tree
	private static void visitSolidArchive(	@NotNull SolidArchiveContext context,
											@NotNull FileFolder archiveFolder,
											@NotNull SolidArchiveTree<?> archiveTree)
			throws IndexingException {
		assert !archiveFolder.hasErrors();
		FileFolder newArchiveFolder = archiveTree.getArchiveFolder();
		try {
			// Collect files to unpack
			visitSolidArchiveFolder(
				context, archiveTree, archiveFolder, newArchiveFolder);
			List<TreeNode> unpackList = context.getUnpackList();
			if (unpackList.isEmpty())
				return;

			// Unpack added and modified files
			context.info(InfoType.UNPACKING, archiveFolder);
			archiveTree.unpack(unpackList, null);
		}
		catch (IOException e) {
			archiveFolder.removeChildren();
			context.fail(ErrorType.ARCHIVE, archiveFolder, e);
			return;
		}
		catch (DiskSpaceException e) {
			archiveFolder.removeChildren();
			context.fail(
				ErrorType.ARCHIVE_UNPACK_DISKSPACE, archiveFolder, e);
			return;

		}
		finally {
			/*
			 * Close archive, possibly delete it, then continue parsing the
			 * unpacked files.
			 */
			Closeables.closeQuietly(archiveTree);
			if (context.isTempArchive())
				archiveTree.getArchiveFile().delete();
		}

		// Process unpacked documents
		indexUnpackedDocs(context, archiveTree, true);
		indexUnpackedDocs(context, archiveTree, false);

		/*
		 * Note: Processing the unpacked archives after all unpacked documents
		 * have been processed and deleted gives us more disk space for the
		 * archive processing, thus reducing the risk of running out of disk
		 * space in case we need to unpack anything from those archives.
		 */

		// Process unpacked nested archives
		for (FileFolder archive : context.nestedArchives.keySet()) {
			assert archive.isArchive();
			if (context.isStopped())
				// Detach archive from tree
				context.nestedArchives.get(archive).removeSubFolder(archive);
			else
				// Recurse into archive
				switchSolidToArchive(context, archiveTree, archive);
		}

		// Clean up caches (not really necessary)
		context.addedDocs.clear();
		context.modifiedDocs.clear();
		context.nestedArchives.clear();

		// Clean up unprocessed temporary files
		if (context.isStopped())
			archiveTree.deleteUnpackedFiles();
	}

	@RecursiveMethod
	private static void visitSolidArchiveFolder(@NotNull final SolidArchiveContext context,
	                                            @NotNull final SolidArchiveTree<?> archiveTree,
												@NotNull final FileFolder oldFolder,
												@NotNull FileFolder newFolder)
			throws IndexingException {
		assert !oldFolder.hasErrors();
		assert !newFolder.hasErrors();
		
		/*
		 * Run document diff
		 */
		new MapValueDiff<String, FileDocument, IndexingException>(
			oldFolder.getDocumentMap(), newFolder.getDocumentMap()) {
			// Missing documents
			protected void handleOnlyLeft(@NotNull FileDocument doc) {
				// Must retrieve UID before detaching from parent
				String uniqueId = doc.getUniqueId();
				oldFolder.removeDocument(doc);
				try {
					context.deleteFromIndex(uniqueId);
				}
				catch (IndexingException e) {
					stop(e);
				}
			}

			// New documents
			protected void handleOnlyRight(@NotNull FileDocument doc) {
				oldFolder.putDocument(doc);
				if (!archiveTree.isEncrypted(doc))
					context.addedDocs.put(doc, oldFolder);
			}

			// Already registered documents
			protected void handleBoth(	@NotNull FileDocument oldDoc,
										@NotNull FileDocument newDoc) {
				if (!oldDoc.isModified(newDoc)) return;
				/*
				 * We'll replace the old document with the new one to update the
				 * last modified field and, in case of HTML pairs, to update the
				 * contents of the HTML folder.
				 */
				oldFolder.putDocument(newDoc);
				if (!archiveTree.isEncrypted(newDoc))
					context.modifiedDocs.put(newDoc, oldFolder);
			}
		}.run();

		/*
		 * Run subfolder diff
		 */
		new MapValueDiff<String, FileFolder, IndexingException>(
			oldFolder.getSubFolderMap(), newFolder.getSubFolderMap()) {
			// Missing subfolders
			protected void handleOnlyLeft(@NotNull FileFolder oldSubFolder) {
				try {
					detachMissingSubFolder(context, oldFolder, oldSubFolder);
				}
				catch (IndexingException e) {
					stop(e); // stop subfolder diff
				}
			}

			// New subfolders
			protected void handleOnlyRight(@NotNull FileFolder newSubFolder) {
				oldFolder.putSubFolder(newSubFolder);
				if (newSubFolder.isArchive()) {
					if (!archiveTree.isEncrypted(newSubFolder))
						context.nestedArchives.put(newSubFolder, oldFolder);
				}
				else {
					new FileFolderVisitor<Exception>(newSubFolder) {
						public void visitDocument(	FileFolder parent,
													FileDocument fileDocument) {
							if (!archiveTree.isEncrypted(fileDocument))
								context.addedDocs.put(fileDocument, parent);
						}

						protected void visitFolder(	FileFolder parent,
													FileFolder folder) {
							if (folder.isArchive() && !archiveTree.isEncrypted(folder))
								context.nestedArchives.put(folder, parent);
						}
					}.runSilently();
				}
			}

			// Already registered subfolders
			protected void handleBoth(	@NotNull FileFolder oldSubFolder,
										@NotNull FileFolder newSubFolder) {
				Long leftModified = oldSubFolder.getLastModified();
				Long rightModified = newSubFolder.getLastModified();
				oldSubFolder.setLastModified(rightModified);
				if (rightModified == null) { // recurse into ordinary folder
					try {
						visitSolidArchiveFolder(
							context, archiveTree, oldSubFolder, newSubFolder);
					}
					catch (IndexingException e) {
						stop(e);
					}
				}
				else if (!rightModified.equals(leftModified)) {
					/*
					 * Recurse into new archive later, skip unmodified archives.
					 * We're storing the old subfolder here and discard the new
					 * subfolder because we can only descend into the old
					 * subfolder - the new one shouldn't have any children at
					 * this point, as the corresponding child files are still
					 * compressed.
					 */
					if (!archiveTree.isEncrypted(oldSubFolder))
						context.nestedArchives.put(oldSubFolder, oldFolder);
				}
			}
		}.run();
	}

	private static void indexUnpackedDocs(	@NotNull SolidArchiveContext context,
											@NotNull final SolidArchiveTree<?> archiveTree,
											boolean added)
			throws IndexingException {
		Map<FileDocument, FileFolder> docToParentMap = added
			? context.addedDocs
			: context.modifiedDocs;

		for (FileDocument doc : docToParentMap.keySet()) {
			if (context.isStopped()) {
				// Detach document from tree
				docToParentMap.get(doc).removeDocument(doc);
				continue;
			}

			File mainFile = archiveTree.getFile(doc);

			FileFolder htmlFolder = doc.getHtmlFolder();
			if (htmlFolder == null) {
				context.indexAndDeleteFile(doc, mainFile, added);
				continue;
			}

			/*
			 * If indexing of the HTML file fails, don't index the files in the
			 * HTML folder, just delete them.
			 */
			final AppendingContext subContext = new AppendingContext(context);
			if (!subContext.indexAndDeleteFile(doc, mainFile, true)) {
				new FileFolderVisitor<Exception>(htmlFolder) {
					protected void visitDocument(	FileFolder parent,
													FileDocument fileDocument) {
						archiveTree.getFile(fileDocument).delete();
					}
				}.runSilently();
				continue;
			}

			subContext.setReporter(null);
			new FileFolderVisitor<IndexingException>(htmlFolder) {
				public void visitDocument(	FileFolder parent,
											FileDocument fileDocument) {
					File file = archiveTree.getFile(fileDocument);
					try {
						subContext.indexAndDeleteFile(fileDocument, file, true);
					}
					catch (IndexingException e) {
						stop(e);
					}
				}

				public void visitFolder(FileFolder parent, FileFolder folder) {
					if (!folder.isArchive()) return;
					try {
						switchSolidToArchive(subContext, archiveTree, folder);
					}
					catch (IndexingException e) {
						stop(e);
					}
				}
			}.run();

			subContext.appendToOuter(doc, added);
		}
	}

	// will delete file
	private static void switchSolidToArchive(	@NotNull FileContext context,
												@NotNull SolidArchiveTree<?> archiveTree,
												@NotNull FileFolder archive)
			throws IndexingException {
		File unpackedFile = archiveTree.getFile(archive);
		assert unpackedFile != null;

		// Wrapping the unpacked archive in a TrueZIP file is necessary for zip
		// recursion
		unpackedFile = new TFile(unpackedFile, context.getZipDetector());
		String archiveName = archive.getName();
		IndexingConfig config = context.getConfig();
		archive.setError(null);

		/*
		 * The user-defined zip extensions have higher priority, so we'll check
		 * for zip archives first.
		 */
		if (unpackedFile.isDirectory()) { // Zip file
			FileContext subContext = new FileContext(context, archive.getPath());
			try {
				visitDirOrZip(subContext, archive, unpackedFile);
			}
			finally {
				unpackedFile.delete();
			}
			return;
		}

		SolidArchiveFactory factory = config.getSolidArchiveFactory(archiveName);
		SolidArchiveContext subContext = new SolidArchiveContext(
			context, archive.getPath(), true);
		try {
			SolidArchiveTree<?> subTree = factory.createSolidArchiveTree(
				subContext, unpackedFile);
			visitSolidArchive(subContext, archive, subTree);
		}
		catch (IOException e) {
			archive.removeChildren();
			ErrorType errorType = Util.hasExtension(archiveName, "exe")
				? ErrorType.NOT_AN_ARCHIVE
				: ErrorType.ARCHIVE;
			context.fail(errorType, archive, e);
		}
		catch (ArchiveEncryptedException e) {
			archive.removeChildren();
			context.fail(ErrorType.ARCHIVE_ENCRYPTED, archive, e);
		}
	}

}
