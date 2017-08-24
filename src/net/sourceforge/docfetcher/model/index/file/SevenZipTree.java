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
import java.util.Map;
import java.util.NoSuchElementException;

import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import SevenZip.Archive.IInArchive;
import SevenZip.Archive.SevenZipEntry;
import SevenZip.Archive.SevenZip.Handler;

import com.google.common.collect.Maps;

final class SevenZipTree extends SolidArchiveTree <SevenZipEntry> {
	
	private IInArchive archive;
	
	public SevenZipTree(@NotNull File archiveFile,
	                    @NotNull IndexingConfig config,
                        @Nullable Path originalPath,
						@Nullable FailReporter failReporter) throws IOException,
			ArchiveEncryptedException {
		super(archiveFile, config, originalPath, failReporter);
	}
	
	// original path may be null -> no enclosing archive, e.g.
	// /home/username/outer-archive.7z
	// otherwise original path must be the "real" absolute path of this archive,
	// e.g.
	// /home/username/outer-archive.7z/inner-archive.7z
	// isStoreRelativePaths only has effect if originalPath is null
	// user is responsible for closing the archive via this.close()
	// Note: Cannot handle TrueZIP archive entries!!!
	public SevenZipTree(@NotNull File archiveFile,
	                    @NotNull IndexingConfig config,
	                    boolean isHtmlPairing,
                        @Nullable Path originalPath,
						@Nullable FailReporter failReporter) throws IOException,
			ArchiveEncryptedException {
		super(archiveFile, config, isHtmlPairing, originalPath, failReporter);
	}
	
	protected ArchiveIterator<SevenZipEntry> getArchiveIterator(File archiveFile,
																String archivePath)
			throws IOException, ArchiveEncryptedException {
		if (archive == null) {
			SevenZipInputStream istream = new SevenZipInputStream(archiveFile);
			archive = new Handler();
			try {
				if (archive.Open(istream) != 0) {
					archive.close();
					throw new IOException();
				}
			}
			catch (IOException e) {
				archive.close();
				if ("k_7zAES not implemented".equals(e.getMessage())) {
					throw new ArchiveEncryptedException(
						archiveFile, archivePath);
				}
				throw e;
			}
		}
		return new ArchiveIterator<SevenZipEntry>() {
			private int index = 0;
			private int size = archive.size();
			public boolean hasNext() {
				return index < size;
			}
			public SevenZipEntry next() {
				if (index >= size)
					throw new NoSuchElementException();
				return archive.getEntry(index++);
			}
			public void finished() {
				// do nothing
			}
		};
	}
	
	protected ArchiveEntryReader<SevenZipEntry> getArchiveEntryReader() {
		return SevenZipEntryReader.instance;
	}
	
	public void close() throws IOException {
		archive.close();
	}
	
	protected Map<Integer, File> doUnpack(	Map<Integer, TreeNode> unpackMap,
											TempFileFactory tempFileFactory)
			throws IOException {
		// Put indices in an int array
		int[] indices = new int[unpackMap.size()];
		int i = 0;
		for (Integer index : unpackMap.keySet())
			indices[i++] = index;
		
		// Unpack files
		Unpacker unpacker = new Unpacker(unpackMap, tempFileFactory);
		return unpacker.unpack(indices);
	}
	
	private static final class SevenZipEntryReader implements
			ArchiveEntryReader<SevenZipEntry> {
		private static final SevenZipEntryReader instance = new SevenZipEntryReader();
		@Nullable
		public String getInnerPath(SevenZipEntry entry) {
			/*
			 * This should return a path relative to the archive root that
			 * always uses forward slashes as separators, even on Windows.
			 * 
			 * Bug #1369: Entry name is null if the entry was created from
			 * standard input. Example on Linux:
			 * echo "Hello World" | 7za a -si test.7z
			 */
			String path = entry.getName();
			assert path == null || (! path.contains("\\") && ! path.startsWith("/"));
			return path;
		}
		public long getLastModified(SevenZipEntry entry) {
			return entry.getTime();
		}
		public boolean isDirectory(SevenZipEntry entry) {
			return entry.isDirectory();
		}
		public long getUnpackedSize(SevenZipEntry entry) {
			return entry.getSize();
		}
		public boolean isEncrypted(SevenZipEntry entry) {
			// J7Zip doesn't provide us with enough info to implement this
			return false;
		}
	}

	private class Unpacker extends SevenZipUnpacker <Map<Integer, File>> {
		private final Map<Integer, TreeNode> unpackMap;
		private final TempFileFactory tempFileFactory;
		private Map<Integer, File> indexFileMap = Maps.newHashMap();
		
		public Unpacker(Map<Integer, TreeNode> unpackMap, TempFileFactory tempFileFactory) {
			super(archive);
			this.unpackMap = unpackMap;
			this.tempFileFactory = tempFileFactory;
		}
		
		public File getOutputFile(int index) throws IOException {
			TreeNode treeNode = unpackMap.get(index);
			try {
				File file = tempFileFactory.createTempFile(treeNode);
				indexFileMap.put(index, file);
				return file;
			} catch (IndexingException e) {
				failReporter.fail(ErrorType.ARCHIVE_ENTRY, treeNode, e);
				throw e.getIOException();
			}
		}
		
		public Map<Integer, File> getUnpackResult() {
			return indexFileMap;
		}
	}
	
}