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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.Folder;
import net.sourceforge.docfetcher.model.Folder.FolderEvent;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.index.DiskSpaceException;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.model.index.PatternAction.MatchAction;
import net.sourceforge.docfetcher.model.index.file.FileFolder.FileFolderVisitor;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.util.collect.SafeKeyMap;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

/**
 * Generic parameter E is the type of the archive entries.
 * Tree is build in constructor; encrypted archive entries are reported and omitted.
 * All other events should be reported by caller.
 * 
 * @author Tran Nam Quang
 */
abstract class SolidArchiveTree<E> implements Closeable {
	
	public interface FailReporter {
		void fail(	ErrorType type,
					TreeNode treeNode,
					@Nullable Throwable cause);
	}
	
	private static final class NullFailReporter implements FailReporter {
		public void fail(	ErrorType type,
							TreeNode treeNode,
							Throwable cause) {
		}
	}
	
	protected interface ArchiveIterator<E> {
		public boolean hasNext();
		@NotNull public E next();
		/**
		 * This method is called when the end of the iteration has been reached.
		 * This gives the implementor the chance to perform cleanup operations
		 * after the iteration.
		 */
		public void finished();
	}
	
	protected interface ArchiveEntryReader<E> {
		/**
		 * Should return a path that is relative to the archive root, always uses
		 * the forward slashes as separator and does not start with a separator. In
		 * other words, it should always look like this: path/to/entry
		 */
		public String getInnerPath(E entry);
		public boolean isDirectory(E entry);
		public long getLastModified(E entry);
		public long getUnpackedSize(E entry);
		public boolean isEncrypted(E entry);
	}
	
	protected class TempFileFactory {
		@NotNull
		public File createTempFile(@NotNull TreeNode treeNode)
				throws IndexingException {
			return config.createDerivedTempFile(treeNode.getName());
		}
	}
	
	private static final class EntryData {
		private int index; // archive entry index
		private long size; // uncompressed filesize
		private final String innerPath;
		private final boolean isEncrypted;
		@Nullable private File file; // unpacked temporary file

		public EntryData(	int index,
							long size,
							@NotNull String innerPath,
							boolean isEncrypted) {
			this.index = index;
			this.size = size;
			this.innerPath = innerPath;
			this.isEncrypted = isEncrypted;
		}
	}
	
	/*
	 * TODO post-release-1.1: The entry data map still uses Path objects as key.
	 * This may not be optimal anymore now that the Path is only stored for root
	 * Folders. (Perhaps this inefficiency has no measurable impact anyway?)
	 */
	
	private final FileFolder archiveFolder;
	private final SafeKeyMap<Path, EntryData> entryDataMap = SafeKeyMap.createHashMap();
	private final TempFileFactory defaultTempFileFactory = new TempFileFactory();
	private final IndexingConfig config;
	protected final FailReporter failReporter;
	protected final File archiveFile;
	
	public SolidArchiveTree(@NotNull File archiveFile,
	                        @NotNull IndexingConfig config,
	                        @Nullable Path originalPath,
							@Nullable FailReporter failReporter)
			throws IOException, ArchiveEncryptedException {
		this(archiveFile, config, config.isHtmlPairing(), originalPath, failReporter);
	}
	
	public SolidArchiveTree(@NotNull File archiveFile,
	                        @NotNull final IndexingConfig config,
	                        boolean isHtmlPairing,
	                        @Nullable Path originalPath,
							@Nullable FailReporter failReporter)
			throws IOException, ArchiveEncryptedException {
		Util.checkNotNull(archiveFile, config);
		this.archiveFile = archiveFile;
		this.config = config;
		this.failReporter = failReporter == null ? new NullFailReporter() : failReporter;
		
		final Path archivePath;
		if (originalPath == null)
			archivePath = config.getStorablePath(archiveFile);
		else
			archivePath = originalPath;
		
		/*
		 * Note: The last-modified value of the folder can be null because it
		 * will not be inserted into the persistent tree structure anyway.
		 */
		archiveFolder = new FileFolder(archivePath, null);
		
		ArchiveIterator<E> archiveIt = getArchiveIterator(
			archiveFile, archivePath.getPath());
		ArchiveEntryReader<E> entryReader = getArchiveEntryReader();
		
		// Build tree structure from flat list of paths
		for (int i = 0; archiveIt.hasNext(); i++) {
			E entry = archiveIt.next();
			FileFolder parent = archiveFolder;
			final String innerPath = entryReader.getInnerPath(entry);
			
			Iterator<String> it = Util.splitPath(innerPath).iterator();
			while (it.hasNext()) { // iterate over inner path parts
				String innerPart = it.next();
				
				// Inner path part corresponds to a directory
				if (it.hasNext() || entryReader.isDirectory(entry)) {
					FileFolder child = parent.getSubFolder(innerPart);
					if (child == null)
						child = new FileFolder(parent, innerPart, null);
					parent = child;
				}
				// Inner path part corresponds to a file
				else {
					TreeNode childNode;
					long lastModified = entryReader.getLastModified(entry);
					if (config.isArchive(innerPart))
						childNode = new FileFolder(
							parent, innerPart, lastModified);
					else
						childNode = new FileDocument(
							parent, innerPart, lastModified);
					Path childPath = childNode.getPath();
					
					long unpackedSize = entryReader.getUnpackedSize(entry);
					boolean isEncrypted = entryReader.isEncrypted(entry);
					if (isEncrypted)
						failReporter.fail(ErrorType.ARCHIVE_ENTRY_ENCRYPTED, childNode, null);
					
					EntryData entryData = new EntryData(
						i, unpackedSize, innerPath, isEncrypted);
					entryDataMap.put(childPath, entryData);
				}
			}
		}
		
		archiveIt.finished();
		
		// HTML pairing
		if (isHtmlPairing)
			applyHtmlPairing(archiveFolder);
		
		// Apply filters; this should be done after the HTML pairing
		applyFilter(archiveFolder, new Predicate<FileDocument>() {
			public boolean apply(FileDocument candidate) {
				String name = candidate.getName();
				Path path = candidate.getPath();
				
				for (PatternAction patternAction : config.getPatternActions()) {
					switch (patternAction.getAction()) {
					case EXCLUDE:
						if (patternAction.matches(name, path, true)) {
							entryDataMap.removeKey(path);
							return true;
						}
						break;
					case DETECT_MIME:
						/*
						 * If the mime pattern matches, we'll check the mime pattern
						 * again later, right before parsing.
						 */
						if (patternAction.matches(name, path, true))
							return false;
						break;
					default:
						throw new IllegalStateException();
					}
				}
				
				if (!ParseService.canParseByName(config, name)) {
					entryDataMap.removeKey(path);
					return true;
				}
				return false;
			}
		}, new Predicate<FileFolder>() {
			public boolean apply(FileFolder candidate) {
				String name = candidate.getName();
				Path path = candidate.getPath();
				boolean isArchive = candidate.isArchive();
				
				for (PatternAction patternAction : config.getPatternActions()) {
					if (patternAction.getAction() == MatchAction.EXCLUDE) {
						if (patternAction.matches(name, path, isArchive)) {
							if (isArchive)
								entryDataMap.removeKey(path);
							return true;
						}
					}
				}
				return false;
			}
		});
	}
	
	// Implementor is expected to close any open resources in case of failure
	@NotNull
	protected abstract ArchiveIterator<E> getArchiveIterator(	File archiveFile,
																String archivePath)
			throws IOException, ArchiveEncryptedException;
	
	@NotNull
	protected abstract ArchiveEntryReader<E> getArchiveEntryReader();
	
	public abstract void close() throws IOException;

	@RecursiveMethod
	private static void applyHtmlPairing(@NotNull FileFolder folder) {
		for (FileFolder subFolder : folder.getSubFolders()) {
			String basename = HtmlUtil.getHtmlDirBasename(subFolder.getName());
			if (basename == null) {
				applyHtmlPairing(subFolder);
				continue;
			}
			boolean isHtmlFolder = false;
			for (String htmlExt : ProgramConf.StrList.HtmlExtensions.get()) { // TODO post-release-1.1: set html extensions?
				String filename = basename + "." + htmlExt;
				FileDocument htmlEntry = folder.getDocument(filename);
				if (htmlEntry == null)
					continue; // current folder is not an HTML folder
				
				// Attach HTML folder to HTML file
				htmlEntry.setHtmlFolder(subFolder);
				
				/*
				 * Detach HTML folder from previous parent folder. This won't
				 * throw a ConcurrentModificationException since we're iterating
				 * over a copy of the subfolders.
				 */
				folder.removeSubFolder(subFolder);
				
				Folder.evtFolderRemoved.fire(new FolderEvent(folder, subFolder));
				isHtmlFolder = true;
				break;
			}
			if (! isHtmlFolder)
				applyHtmlPairing(subFolder);
		}
	}
	
	@RecursiveMethod
	private static void applyFilter(@NotNull FileFolder folder,
									@NotNull Predicate<FileDocument> docPredicate,
									@NotNull Predicate<FileFolder> folderPredicate) {
		folder.removeDocuments(docPredicate);
		folder.removeSubFolders(folderPredicate);
		for (FileDocument doc : folder.getDocuments()) {
			FileFolder htmlFolder = doc.getHtmlFolder();
			if (htmlFolder != null)
				applyFilter(htmlFolder, docPredicate, folderPredicate);
		}
		for (FileFolder subFolder : folder.getSubFolders())
			applyFilter(subFolder, docPredicate, folderPredicate);
	}
	
	public final void unpack(@NotNull TreeNode unpackEntry) throws IOException,
			DiskSpaceException {
		unpack(Collections.singleton(unpackEntry), null);
	}
	
	// Caller is responsible for deleting the files (can use deleteUnpackedFiles for that).
	// If the given list of unpack entries contains HTML files, the files in the HTML folder will be unpacked as well
	// If tempDir is given, the unpack operation preserves the inner directory structure, so
	// that tempDir corresponds to archive root;
	// otherwise the archive entries are unpacked to independently chosen temp files.
	// If tempDir is given, caller is responsible for deleting it and everything underneath it.
	// Tip: Use UtilGlobal.convert in case of incompatible Collection types
	public final void unpack(	@NotNull Iterable<? extends TreeNode> unpackEntries,
								@Nullable final File tempDir)
			throws IOException, DiskSpaceException {
		final long[] requiredSpace = { 0 };
		final Map<Integer, TreeNode> unpackMap = Maps.newHashMap();
		
		// Collect entries to unpack, calculate required diskspace
		for (TreeNode entry : unpackEntries) {
			EntryData entryData = entryDataMap.getValue(entry.getPath());
			unpackMap.put(entryData.index, entry);
			requiredSpace[0] += entryData.size;
			
			// Unpack files under HTML folders if there are any
			if (!hasHtmlFolder(entry))
				continue;
			new FileFolderVisitor<Exception>((FileDocument) entry) {
				protected void visitDocument(	FileFolder parent,
				                             	FileDocument fileDocument) {
					Path path = fileDocument.getPath();
					EntryData entryData = entryDataMap.getValue(path);
					unpackMap.put(entryData.index, fileDocument);
					requiredSpace[0] += entryData.size;
				}
			}.runSilently();
		}
		
		/*
		 * We can't check emptiness on the given Iterable in an efficient way,
		 * so we'll do it here.
		 */
		if (unpackMap.isEmpty()) return;
		
		// Fail if there's not enough disk space for unpacking
		config.checkDiskSpaceInTempDir(requiredSpace[0]);
		
		// Create temporary file factory
		final TempFileFactory tempFileFactory;
		if (tempDir == null) {
			tempFileFactory = defaultTempFileFactory;
		}
		else {
			assert tempDir.isDirectory();
			tempFileFactory = new TempFileFactory() {
				private String tempDirPath = Util.getAbsPath(tempDir);
				public File createTempFile(TreeNode treeNode) throws IndexingException {
					EntryData entryData = entryDataMap.getValue(treeNode.getPath());
					String tempFilePath = Util.joinPath(tempDirPath, entryData.innerPath);
					File file = new File(tempFilePath);
					try {
						Files.createParentDirs(file);
					}
					catch (IOException e) {
						throw new IndexingException(e);
					}
					return file;
				}
			};
		}
		
		// Unpack files
		final Map<Integer, File> indexFileMap = doUnpack(unpackMap, tempFileFactory);
		
		// Store the unpacked entries
		for (TreeNode entry : unpackEntries) {
			EntryData entryData = entryDataMap.getValue(entry.getPath());
			entryData.file = indexFileMap.get(entryData.index); // file may be null
			
			// Store unpacked entries underneath HTML folders
			if (!hasHtmlFolder(entry))
				continue;
			new FileFolderVisitor<Exception>((FileDocument) entry) {
				protected void visitDocument(	FileFolder parent,
				                             	FileDocument fileDocument) {
					Path path = fileDocument.getPath();
					EntryData entryData = entryDataMap.getValue(path);
					entryData.file = indexFileMap.get(entryData.index); // file may be null
				}
			}.runSilently();
		}
	}
	
	private static boolean hasHtmlFolder(@NotNull TreeNode treeNode) {
		if (!(treeNode instanceof FileDocument))
			return false;
		return ((FileDocument) treeNode).getHtmlFolder() != null;
	}
	
	// Subclasser should not report anything except failure on single archive entries
	// Subclasser can use this.getTreeNode(int) to get the document associated with an index,
	// which is useful for naming temporary files
	// The list of indices may not be sorted
	// Subclasser is allowed to modify the unpackMap
	@NotNull
	protected abstract Map<Integer, File> doUnpack(	@NotNull Map<Integer, TreeNode> unpackMap,
													@NotNull TempFileFactory tempFileFactory)
			throws IOException;
	
	@NotNull
	public final File getArchiveFile() {
		return archiveFile;
	}
	
	@NotNull
	public final FileFolder getArchiveFolder() {
		return archiveFolder;
	}
	
	// Only the path of the given object is used to retrieve the file.
	// The returned file is null if the unpacking failed for some reason, e.g. corrupted data.
	@Nullable
	public final File getFile(@NotNull TreeNode treeNode) {
		EntryData entryData = entryDataMap.getValue(treeNode.getPath());
		if (entryData == null)
			return null;
		return entryData.file;
	}
	
	public final boolean isEncrypted(@NotNull TreeNode treeNode) {
		EntryData entryData = entryDataMap.getValue(treeNode.getPath());
		if (entryData == null)
			return false;
		return entryData.isEncrypted;
	}
	
	public final void deleteUnpackedFiles() {
		for (EntryData entryData : entryDataMap.values())
			if (entryData.file != null)
				entryData.file.delete();
	}
	
	// Returns the path of the given archive entry relative to the archive
	@Nullable
	public final String getArchiveEntryPath(@NotNull TreeNode treeNode) {
		EntryData entryData = entryDataMap.getValue(treeNode.getPath());
		if (entryData == null)
			return null;
		return entryData.innerPath;
	}

}
