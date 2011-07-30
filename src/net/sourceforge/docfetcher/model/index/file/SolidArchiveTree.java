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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.Folder.Predicate;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.index.DiskSpaceException;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingReporter.ErrorType;
import net.sourceforge.docfetcher.model.index.file.FileFolder.FileFolderVisitor;
import net.sourceforge.docfetcher.model.parse.ParseService;

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
		public boolean isEncrypted();
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
		@Nullable private File file; // unpacked temporary file
		public EntryData(int index, long size, @NotNull String innerPath) {
			this.index = index;
			this.size = size;
			this.innerPath = innerPath;
		}
	}
	
	/**
	 * A subclass of HashMap that deprecates the {@link Map#get(Object)} method
	 * in favor of the more restrictive {@link SafeKeyMap#getValue(K)}. This
	 * class is useful in situations where it is very easy to use the wrong map
	 * key, e.g. a File object instead of the file path.
	 */
	private static final class SafeKeyMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 1L;
		public static <K, V> SafeKeyMap<K, V> create() {
			return new SafeKeyMap<K, V> ();
		}
		@Deprecated
		public V get(Object key) {
			throw new UnsupportedOperationException();
		}
		public V getValue(K key) {
			return super.get(key);
		}
	}
	
	private final FileFolder archiveFolder;
	private final SafeKeyMap<String, EntryData> entryDataMap = SafeKeyMap.create();
	private final TempFileFactory defaultTempFileFactory = new TempFileFactory();
	private final IndexingConfig config;
	protected final FailReporter failReporter;
	protected final File archiveFile;
	
	public SolidArchiveTree(@NotNull File archiveFile,
	                        @NotNull IndexingConfig config,
	                        @Nullable String originalPath,
							@Nullable FailReporter failReporter)
			throws IOException, ArchiveEncryptedException {
		this(archiveFile, config, config.isHtmlPairing(), originalPath, failReporter);
	}
	
	public SolidArchiveTree(@NotNull File archiveFile,
	                        @NotNull final IndexingConfig config,
	                        boolean isHtmlPairing,
	                        @Nullable String originalPath,
							@Nullable FailReporter failReporter)
			throws IOException, ArchiveEncryptedException {
		Util.checkNotNull(archiveFile, config);
		this.archiveFile = archiveFile;
		this.config = config;
		this.failReporter = failReporter == null ? new NullFailReporter() : failReporter;
		
		final String archiveName;
		final String archivePath;
		if (originalPath == null) {
			archiveName = archiveFile.getName();
			archivePath = config.getStorablePath(archiveFile);
		} else {
			List<String> pathParts = Util.splitPath(originalPath);
			archiveName = Util.getLast(pathParts);
			archivePath = originalPath;
		}
		
		/*
		 * Note: The last-modified value of the folder can be null because it
		 * will not be inserted into the persistent tree structure anyway.
		 */
		archiveFolder = new FileFolder(archiveName, archivePath, null);
		
		ArchiveIterator<E> archiveIt = getArchiveIterator(archiveFile);
		if (archiveIt.isEncrypted())
			throw new ArchiveEncryptedException(archiveFile, archiveFolder.getPath());
		ArchiveEntryReader<E> entryReader = getArchiveEntryReader();
		
		// Build tree structure from flat list of paths
		for (int i = 0; archiveIt.hasNext(); i++) {
			E entry = archiveIt.next();
			if (entryReader.isEncrypted(entry)) {
				/*
				 * The name and path of the archive entry may be encrypted,
				 * so we'll report the name and path of the archive.
				 */
				failReporter.fail(ErrorType.ARCHIVE_ENTRY_ENCRYPTED, archiveFolder, null);
				continue;
			}
			FileFolder parent = archiveFolder;
			
			String innerPath = entryReader.getInnerPath(entry);
			String intermediatePath = archiveFolder.getPath();
			
			Iterator<String> it = Util.splitPath(innerPath).iterator();
			while (it.hasNext()) { // iterate over inner path parts
				String innerPart = it.next();
				if (it.hasNext() || entryReader.isDirectory(entry)) { // inner path part corresponds to a directory
					FileFolder child = parent.getSubFolder(innerPart);
					if (child == null) {
						intermediatePath = Util.joinPath(intermediatePath, innerPart);
						child = new FileFolder(innerPart, intermediatePath, null);
						parent.putSubFolder(child);
					} else {
						intermediatePath = child.getPath();
					}
					parent = child;
				} else { // inner path part corresponds to a file
					long lastModified = entryReader.getLastModified(entry);
					final String childPath;
					if (config.isArchive(innerPart)) {
						String fullPath = Util.joinPath(
							archiveFolder.getPath(), innerPath);
						FileFolder child = new FileFolder(
							innerPart, fullPath, lastModified);
						parent.putSubFolder(child);
						childPath = child.getPath();
					} else {
						FileDocument child = new FileDocument(
							parent, innerPart, lastModified);
						childPath = child.getPath();
					}
					long unpackedSize = entryReader.getUnpackedSize(entry);
					EntryData entryData = new EntryData(i, unpackedSize, innerPath);
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
			public boolean matches(FileDocument candidate) {
				String name = candidate.getName();
				String path = candidate.getPath();
				if (config.getFileFilter().matches(name, path, true)) {
					entryDataMap.remove(candidate.getPath());
					return true;
				}
				/*
				 * If the mime pattern matches, we'll check the mime pattern
				 * again later, right before parsing.
				 */
				if (config.matchesMimePattern(name) || ParseService.canParseByName(config, name))
					return false;
				entryDataMap.remove(candidate.getPath());
				return true;
			}
		}, new Predicate<FileFolder>() {
			public boolean matches(FileFolder candidate) {
				boolean isArchive = candidate.isArchive();
				boolean matches = config.getFileFilter().matches(
						candidate.getName(), candidate.getPath(), isArchive
				);
				if (matches && isArchive)
					entryDataMap.remove(candidate.getPath());
				return matches;
			}
		});
	}
	
	@NotNull
	protected abstract ArchiveIterator<E> getArchiveIterator(File archiveFile)
			throws IOException;
	
	@NotNull
	protected abstract ArchiveEntryReader<E> getArchiveEntryReader();
	
	public abstract void close() throws IOException;
	
	@RecursiveMethod
	private static void applyHtmlPairing(@NotNull FileFolder folder) {
		if (folder.subFolders == null) return;
		Iterator<FileFolder> subFolderIt = folder.subFolders.values().iterator();
		while (subFolderIt.hasNext()) {
			FileFolder subFolder = subFolderIt.next();
			String basename = HtmlUtil.getHtmlDirBasename(subFolder.getName());
			if (basename == null) {
				applyHtmlPairing(subFolder);
				continue;
			}
			boolean isHtmlFolder = false;
			for (String htmlExt : ProgramConf.StrArray.HtmlExtensions.get()) { // TODO set html extensions?
				String filename = basename + "." + htmlExt;
				FileDocument htmlEntry = folder.getDocument(filename);
				if (htmlEntry == null)
					continue; // not an HTML folder
				htmlEntry.setHtmlFolder(subFolder); // attach HTML folder to HTML file
				subFolderIt.remove(); // detach HTML folder from previous parent folder
				isHtmlFolder = true;
				break;
			}
			if (! isHtmlFolder)
				applyHtmlPairing(subFolder);
		}
		if (folder.subFolders.isEmpty())
			folder.subFolders = null;
	}
	
	@RecursiveMethod
	private static void applyFilter(@NotNull FileFolder folder,
									@NotNull Predicate<FileDocument> docPredicate,
									@NotNull Predicate<FileFolder> folderPredicate) {
		folder.removeDocuments(docPredicate);
		folder.removeSubFolders(folderPredicate);
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
					String path = fileDocument.getPath();
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
		} else {
			assert tempDir.isDirectory();
			tempFileFactory = new TempFileFactory() {
				private String tempDirPath = Util.getAbsPath(tempDir);
				public File createTempFile(TreeNode treeNode) throws IndexingException {
					EntryData entryData = entryDataMap.getValue(treeNode.getPath());
					String tempFilePath = Util.joinPath(tempDirPath, entryData.innerPath);
					File file = new File(tempFilePath);
					try {
						Files.createParentDirs(file);
					} catch (IOException e) {
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
			entryData.file = indexFileMap.get(entryData.index);
			assert entryData.file != null;
			
			// Store unpacked entries underneath HTML folders
			if (!hasHtmlFolder(entry))
				continue;
			new FileFolderVisitor<Exception>((FileDocument) entry) {
				protected void visitDocument(	FileFolder parent,
				                             	FileDocument fileDocument) {
					String path = fileDocument.getPath();
					EntryData entryData = entryDataMap.getValue(path);
					entryData.file = indexFileMap.get(entryData.index);
					assert entryData.file != null;
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
	
	// Only the path of the given object is used to retrieve the file
	@Nullable
	public final File getFile(@NotNull TreeNode treeNode) {
		EntryData entryData = entryDataMap.getValue(treeNode.getPath());
		if (entryData == null) return null;
		return entryData.file;
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
		if (entryData == null) return null;
		return entryData.innerPath;
	}

}
