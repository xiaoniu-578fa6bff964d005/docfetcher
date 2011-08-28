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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingError.ErrorType;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.NullOutputStream;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

/**
 * @author Tran Nam Quang
 */
final class RarTree extends SolidArchiveTree<FileHeader> {

	public RarTree(@NotNull File archiveFile,
	               @NotNull IndexingConfig config,
                   @Nullable String originalPath,
	               @Nullable FailReporter failReporter)
			throws IOException, ArchiveEncryptedException {
		super(archiveFile, config, originalPath, failReporter);
	}
	
	public RarTree(@NotNull File archiveFile,
	               @NotNull IndexingConfig config,
	               boolean isHtmlPairing,
                   @Nullable String originalPath,
	               @Nullable FailReporter failReporter)
			throws IOException, ArchiveEncryptedException {
		super(archiveFile, config, isHtmlPairing, originalPath, failReporter);
	}
	
	public void close() throws IOException {
		// Do nothing
	}
	
	protected ArchiveIterator<FileHeader> getArchiveIterator(File archiveFile)
			throws IOException {
		try {
			final Archive archive = new Archive(archiveFile);
			return new ArchiveIterator<FileHeader>() {
				private FileHeader nextFileHeader = archive.nextFileHeader();
				public FileHeader next() {
					FileHeader fh = nextFileHeader;
					nextFileHeader = archive.nextFileHeader();
					return fh;
				}
				public boolean hasNext() {
					return nextFileHeader != null;
				}
				public void finished() {
					Closeables.closeQuietly(archive);
				}
				public boolean isEncrypted() {
					return archive.isEncrypted();
				}
			};
		} catch (RarException e) {
			throw new IOException(e);
		}
	}
	
	private static final class RarEntryReader implements ArchiveEntryReader<FileHeader> {
		private static final RarEntryReader instance = new RarEntryReader();
		public String getInnerPath(FileHeader entry) {
			String subPath = entry.isUnicode() ? entry.getFileNameW() : entry.getFileNameString();
			return subPath.replace('\\', '/');
		}
		public boolean isDirectory(FileHeader entry) {
			return entry.isDirectory();
		}
		public long getLastModified(FileHeader entry) {
			return entry.getMTime().getTime();
		}
		public long getUnpackedSize(FileHeader entry) {
			return entry.getUnpSize();
		}
		public boolean isEncrypted(FileHeader entry) {
			return entry.isEncrypted();
		}
	}
	
	protected ArchiveEntryReader<FileHeader> getArchiveEntryReader() {
		return RarEntryReader.instance;
	}
	
	protected Map<Integer, File> doUnpack(	Map<Integer, TreeNode> unpackMap,
											TempFileFactory tempFileFactory)
			throws IOException {
		Map<Integer, File> indexFileMap = Maps.newHashMap();
		Archive archive = null;
		try {
			archive = new Archive(archiveFile);

			/*
			 * If the archive uses solid compression, all files preceding the
			 * target files must be extracted, otherwise JUnRar will throw
			 * errors, such as 'crcError'. In order to save disk space, we can
			 * extract unneeded files into a NullOutputStream.
			 * 
			 * To find out whether the archive uses solid compression, we'll
			 * have to iterate over all file headers and check their solid flags
			 * one by one. - This is necessary, because it is usually not the
			 * case that all file headers have the same solid flag! In fact, in
			 * a regular solid archive the first file header is marked
			 * 'non-solid', while the remaining file headers are marked 'solid'.
			 */
			boolean isSolid = isSolidRarArchive(archive);
			
			FileHeader fh = null;
			NullOutputStream nullOut = isSolid ? new NullOutputStream() : null;
			for (int i = 0;; i++) {
				/*
				 * We can abort early if we've extracted all needed files before
				 * reaching the end of the archive.
				 */
				if (unpackMap.isEmpty())
					break;
				
				fh = archive.nextFileHeader();
				if (fh == null) break; // Last entry reached
				if (fh.isDirectory()) continue;

				/*
				 * This was already reported when the tree was constructed, so
				 * we can continue silently here.
				 */
				if (fh.isEncrypted())
					continue;
				
				/*
				 * Remove entry from map so we'll know when there are no more
				 * files to extract.
				 */
				TreeNode treeNode = unpackMap.remove(i);
				
				try {
					if (treeNode != null) {
						File file = tempFileFactory.createTempFile(treeNode);
						OutputStream out = new FileOutputStream(file);
						archive.extractFile(fh, out);
						Closeables.closeQuietly(out);
						indexFileMap.put(i, file);
					} else if (isSolid) {
						archive.extractFile(fh, nullOut);
					}
				} catch (Exception e) {
					if (treeNode != null) // Ignore errors for entries written to NullOutputStream
						failReporter.fail(ErrorType.ARCHIVE_ENTRY, treeNode, e);
				}
			}
			return indexFileMap;
		} catch (RarException e) {
			throw new IOException(e);
		} finally {
			Closeables.closeQuietly(archive);
		}
	}
	
	private static boolean isSolidRarArchive(@NotNull Archive archive) {
		for (FileHeader fh : archive.getFileHeaders())
			if (fh.isSolid())
				return true;
		return false;
	}

}
