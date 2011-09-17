/*******************************************************************************
 * Copyright (c) 2011 Tran Nam Quang.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.sourceforge.docfetcher.model.HotColdFileCache;
import net.sourceforge.docfetcher.model.HotColdFileCache.PermanentFileResource;
import net.sourceforge.docfetcher.model.FileResource;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.DiskSpaceException;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.file.FileFolder.FileFolderVisitor;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import SevenZip.Archive.IInArchive;
import SevenZip.Archive.SevenZipEntry;
import SevenZip.Archive.SevenZip.Handler;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.NullOutputStream;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsSyncException;

/**
 * @author Tran Nam Quang
 */
public final class FileFactory {
	
	private final HotColdFileCache unpackCache;
	
	public FileFactory(@NotNull HotColdFileCache unpackCache) {
		this.unpackCache = Util.checkNotNull(unpackCache);
	}
	
	// thrown parse exception has localized error message
	@NotNull
	@ThreadSafe
	public FileResource createFile(	@NotNull IndexingConfig config,
									@NotNull String path) throws ParseException {
		Util.checkNotNull(config, path);
		path = UtilModel.normalizePath(path);
		
		try {
			String[] pathParts = UtilModel.splitAtExisting(path, "");
			File leftFile = new File(pathParts[0]);
			
			// Input path refers to an ordinary file;
			// this is the most common case and must therefore return reasonably fast
			if (pathParts[1].length() == 0)
				return new PermanentFileResource(leftFile);
			
			// Input path seems to refer to an archive entry; let's check the cache first
			String archivePath = Util.getAbsPath(leftFile);
			String absInputPath = Util.joinPath(archivePath, pathParts[1]);
			CacheSplitResult splitResult = splitAtCached(absInputPath, "");
			
			// Nothing found in cache; attempt to unpack the archive entry
			if (splitResult == null) {
				FileResource fileResource = new PermanentFileResource(leftFile);
				return unpackFromArchive(config, archivePath, fileResource, pathParts[1]);
			}
			
			// Found unpacked ordinary file in cache; just return it
			if (splitResult.right.length() == 0)
				return splitResult.fileResource;
			
			// Found intermediate archive in cache; need to unpack the remainder
			return unpackFromArchive(
					config,
					splitResult.left,
					splitResult.fileResource,
					splitResult.right
			);
		}
		catch (FileNotFoundException e) {
			throw new ParseException(e); // TODO i18n: add localized error message
		}
		catch (ArchiveEncryptedException e) {
			throw new ParseException(e); // TODO i18n: add localized error message
		}
		catch (DiskSpaceException e) {
			throw new ParseException(e); // TODO i18n: add localized error message
		}
		catch (IOException e) {
			throw new ParseException(e); // TODO i18n: add localized error message
		}
	}
	
	private static class CacheSplitResult {
		private final FileResource fileResource;
		private final String left;
		private final String right;

		public CacheSplitResult(@NotNull FileResource fileResource,
								@NotNull String left,
								@NotNull String right) {
			Util.checkNotNull(fileResource, left, right);
			this.fileResource = fileResource;
			this.left = left;
			this.right = right;
		}
	}
	
	@Nullable
	@RecursiveMethod
	private CacheSplitResult splitAtCached(	@NotNull String left,
											@NotNull String right) {
		FileResource fileResource = unpackCache.get(left);
		if (fileResource != null)
			return new CacheSplitResult(fileResource, left, right);
		if (! left.contains("/") && ! left.contains("\\")) // reached left end of a relative path
			return null;
		String[] leftParts = Util.splitPathLast(left);
		if (leftParts[0].length() == 0) // reached Unix root
			return null;
		if (leftParts[0].matches("[a-zA-Z]:")) // reached Windows root
			return null;
		// Move by one path part to the left and recurse
		if (right.length() == 0)
			return splitAtCached(leftParts[0], leftParts[1]);
		String newRight = Util.joinPath(leftParts[1], right);
		return splitAtCached(leftParts[0], newRight);
	}
	
	@NotNull
	private FileResource unpackFromArchive(	@NotNull IndexingConfig config,
											@NotNull String originalArchivePath,
											@NotNull FileResource archiveResource,
											@NotNull String entryPath)
			throws ArchiveEncryptedException, DiskSpaceException,
			FileNotFoundException, IOException {
		File archiveFile = archiveResource.getFile();
		assert ! (archiveFile instanceof TFile);
		assert archiveFile.isFile();
		TFile tzFile = new TFile(archiveFile, config.createZipDetector());
		if (tzFile.isDirectory())
			return unpackFromZipArchive(config, originalArchivePath, archiveResource, tzFile, entryPath);
		return unpackFromSolidArchive(config, originalArchivePath, archiveResource, entryPath);
	}

	@NotNull
	private FileResource unpackFromSolidArchive(@NotNull IndexingConfig config,
												@NotNull String originalArchivePath,
												@NotNull FileResource archiveResource,
												@NotNull String entryPath)
			throws ArchiveEncryptedException, DiskSpaceException,
			FileNotFoundException, IOException {
		File archiveFile = archiveResource.getFile();
		String archiveExt = Util.splitFilename(archiveFile)[1];
		if (! Util.hasExtension(entryPath, config.getHtmlExtensions())) { // Without HTML pairing
			if (archiveExt.equals("exe") || archiveExt.equals("7z"))
				return unpackFrom7zArchive(config, originalArchivePath, archiveResource, entryPath);
			else if (archiveExt.equals("rar"))
				// JUnRar doesn't support SFX rar archives
				return unpackFromRarArchive(config, originalArchivePath, archiveResource, entryPath);
			else
				throw new FileNotFoundException();
		} else { // With HTML pairing
			/*
			 * Note: We'll ignore the HTML pairing flag in the config object and
			 * always leave the HTML pairing on.
			 */
			SolidArchiveTree<?> solidArchive;
			if (archiveExt.equals("exe") || archiveExt.equals("7z"))
				solidArchive = new SevenZipTree(archiveFile, config, true, originalArchivePath, null);
			else if (archiveExt.equals("rar"))
				// JUnRar doesn't support SFX rar archives
				solidArchive = new RarTree(archiveFile, config, true, originalArchivePath, null);
			else
				throw new FileNotFoundException();
			return unpackFromSolidArchive(config, archiveResource, solidArchive, entryPath);
		}
	}
	
	@NotNull
	private FileResource unpackFromZipArchive(	@NotNull final IndexingConfig config,
												@NotNull final String originalArchivePath,
												@NotNull final FileResource archiveResource,
												@NotNull final TFile archiveFile,
												@NotNull final String entryPath)
			throws ArchiveEncryptedException, DiskSpaceException,
			FileNotFoundException, IOException {
		assert archiveFile.isArchive() && archiveFile.getEnclArchive() == null;
		
		final FileResource[] result = new FileResource[1];
		final Exception[] exception = new Exception[1];
		
		/*
		 * Note: We'll ignore the HTML pairing flag in the config object and
		 * always leave the HTML pairing on.
		 */
		new HtmlFileWalker (archiveFile, config) {
			protected void handleFile(File file) {
				String currentPath = getRelativePath(archiveFile, file);
				try {
					if (currentPath.equals(entryPath)) { // Exact match on regular file
						File unpackedFile = maybeUnpackZipEntry(config, file);
						String cacheKey = Util.joinPath(originalArchivePath, currentPath);
						result[0] = unpackCache.putIfAbsent(cacheKey, unpackedFile);
						stop();
					} else if (entryPath.startsWith(currentPath + "/") // Partial match on solid archive
							&& config.isSolidArchive(file.getName())) {
						File innerArchiveFile;
						try {
							innerArchiveFile = maybeUnpackZipEntry(config, file);
							TFile.umount(archiveFile);
						} finally {
							archiveResource.dispose();
						}
						String cacheKey = Util.joinPath(originalArchivePath, currentPath);
						FileResource innerArchive = unpackCache.putIfAbsent(cacheKey, innerArchiveFile);
						String remainingPath = entryPath.substring(currentPath.length() + 1);
						result[0] = unpackFromSolidArchive(
								config,
								cacheKey,
								innerArchive,
								remainingPath
						);
						stop();
					}
				} catch (Exception e) {
					exception[0] = e;
					stop();
				}
			}
			protected void handleHtmlPair(	File htmlFile,
											File htmlDir) {
				String currentPath = getRelativePath(archiveFile, htmlFile);
				if (! currentPath.equals(entryPath)) return;
				String cacheKey = Util.joinPath(originalArchivePath, currentPath);
				try {
					if (htmlDir == null) {
						File unpackedFile = maybeUnpackZipEntry(config, htmlFile);
						result[0] = unpackCache.putIfAbsent(cacheKey, unpackedFile);
					} else {
						/*
						 * Here, we could check whether there's enough disk
						 * space for unpacking the files, and throw an exception
						 * if not. However, this requires recursing into the
						 * zipped HTML folder and is also very unlikely to
						 * happen, so we'll leave it out.
						 */
						TFile tzHtmlFile = (TFile) htmlFile;
						TFile tzHtmlDir = (TFile) htmlDir;
						File tempDir = Files.createTempDir();
						File newHtmlFile = new File(tempDir, tzHtmlFile.getName());
						File newHtmlDir = new File(tempDir, tzHtmlDir.getName());
						tzHtmlFile.cp(newHtmlFile);
						tzHtmlDir.cp_r(newHtmlDir);
						result[0] = unpackCache.putIfAbsent(cacheKey, newHtmlFile, tempDir);
					}
				} catch (Exception e) {
					exception[0] = e;
				}
				stop();
			}
			protected void handleDir(File dir) {
				// Nothing to do
			}
			protected boolean skip(File fileOrDir) {
				if (! fileOrDir.isDirectory())
					return false;
				// Skip all directories that aren't parent directories of the target file
				String currentPath = getRelativePath(archiveFile, fileOrDir);
				return ! entryPath.startsWith(currentPath + "/");
			}
			protected void runFinally() {
				try {
					if (archiveFile.exists()) // Might have been deleted earlier
						TFile.umount(archiveFile);
				} catch (FsSyncException e) {
					exception[0] = e;
				} finally {
					archiveResource.dispose();
				}
			}
		}.runSilently();
		
		maybeThrow(exception, DiskSpaceException.class);
		maybeThrow(exception, IOException.class);
		maybeThrow(exception, FileNotFoundException.class);
		maybeThrow(exception, ArchiveEncryptedException.class);
		
		/*
		 * If we reach this point with a non-null exception array element, we
		 * might have forgotten to rethrow exceptions of a particular type.
		 */
		assert exception[0] == null;
		
		if (result[0] == null)
			throw new FileNotFoundException();
		
		return result[0];
	}
	
	private static <T extends Throwable> void maybeThrow(	@NotNull Exception[] array,
															@NotNull Class<T> clazz)
			throws T {
		assert array.length == 1;
		if (clazz.isInstance(array[0]))
			throw clazz.cast(array[0]);
	}
	
	@NotNull
	private static String getRelativePath(	@NotNull File src,
											@NotNull File dst) {
		String path = UtilModel.getRelativePath(src, dst);
		assert UtilModel.noTrailingSlash(path);
		return path;
	}
	
	@NotNull
	private static File maybeUnpackZipEntry(@NotNull IndexingConfig config,
											@NotNull File packedFile)
			throws DiskSpaceException, IOException {
		try {
			File unpackedFile = UtilModel.maybeUnpackZipEntry(config, packedFile);
			assert unpackedFile != null;
			return unpackedFile;
		} catch (IndexingException e) {
			throw e.getIOException();
		}
	}
	
	// does not support HTML pairing, but is faster and more lightweight
	@NotNull
	private FileResource unpackFrom7zArchive(	@NotNull final IndexingConfig config,
												@NotNull String originalArchivePath,
												@NotNull FileResource archiveResource,
												@NotNull String entryPath)
			throws ArchiveEncryptedException, DiskSpaceException,
			FileNotFoundException, IOException {
		// TODO now: can we check if the archive or the target archive entry is encrypted?
		assert UtilModel.noTrailingSlash(entryPath);
		
		IInArchive archive = new Handler();
		SevenZipInputStream istream = new SevenZipInputStream(archiveResource.getFile());
		if (archive.Open(istream) != 0)
			throw new IOException();
		
		try {
			for (int i = 0; i < archive.size(); i++) {
				SevenZipEntry entry = archive.getEntry(i);
				if (entry.isDirectory()) continue;
				final String currentPath = entry.getName();
				assert ! currentPath.contains("\\");
				assert UtilModel.noTrailingSlash(currentPath);

				// TODO now: throw disk space exception
				if (entryPath.equals(currentPath)) { // Exact match
					File unpackedFile = unpack7zEntry(config, archive, currentPath, i);
					String cacheKey = Util.joinPath(originalArchivePath, currentPath);
					return unpackCache.putIfAbsent(cacheKey, unpackedFile);
				} else if (entryPath.startsWith(currentPath + "/")
						&& config.isArchive(currentPath)) { // Partial match
					File innerArchiveFile;
					try {
						innerArchiveFile = unpack7zEntry(config, archive, currentPath, i);
					} finally {
						archiveResource.dispose();
					}
					String cacheKey = Util.joinPath(originalArchivePath, currentPath);
					String remainingPath = entryPath.substring(currentPath.length() + 1);
					FileResource innerArchive = unpackCache.putIfAbsent(cacheKey, innerArchiveFile);
					return unpackFromArchive(config, cacheKey, innerArchive, remainingPath);
				}
			}
		} finally {
			archive.close();
			archiveResource.dispose();
		}
		
		throw new FileNotFoundException();
	}
	
	private static File unpack7zEntry(	@NotNull final IndexingConfig config,
										@NotNull IInArchive archive,
										@NotNull final String entryPath,
										int index) throws IOException {
		return new SevenZipUnpacker<File>(archive) {
			private File unpackedFile;
			public File getOutputFile(int index) throws IOException {
				String entryName = getLastPathPart(entryPath);
				try {
					return unpackedFile = config.createDerivedTempFile(entryName);
				} catch (IndexingException e) {
					throw e.getIOException();
				}
			}
			public File getUnpackResult() {
				return unpackedFile;
			}
		}.unpack(index);
	}
	
	// does not support HTML pairing, but is faster and more lightweight
	@NotNull
	private FileResource unpackFromRarArchive(	@NotNull IndexingConfig config,
												@NotNull String originalArchivePath,
												@NotNull FileResource archiveResource,
												@NotNull String entryPath)
			throws ArchiveEncryptedException, DiskSpaceException,
			FileNotFoundException, IOException {
		Archive archive = null;
		try {
			File archiveFile = archiveResource.getFile();
			archive = new Archive(archiveFile);
			if (archive.isEncrypted())
				throw new ArchiveEncryptedException(archiveFile, originalArchivePath);

			List<FileHeader> fileHeaders = archive.getFileHeaders();
			
			boolean isSolid = false;
			for (FileHeader fh : fileHeaders) {
				if (fh.isSolid()) {
					isSolid = true;
					break;
				}
			}
			
			/*
			 * For solid archives, if we want to extract a certain archive
			 * entry, we also have to extract all archive entries that preceded
			 * it. Thus, it is more efficient to run through the archive twice
			 * rather than once: During the first phase, we check for any
			 * matching archive entries by only looking at the file headers, and
			 * return early if there is no match. Only if there is a match,
			 * we'll proceed to the second phase, where, if the archive uses
			 * solid compression, all files up to the target file will be
			 * extracted.
			 */
			if (isSolid) {
				boolean match = false;
				for (FileHeader fh : fileHeaders) {
					if (fh.isEncrypted() || fh.isDirectory())
						continue;
					String currentPath = fh.isUnicode() ? fh.getFileNameW() : fh.getFileNameString();
					currentPath = currentPath.replace('\\', '/');
					assert UtilModel.noTrailingSlash(currentPath);
					
					if (entryPath.equals(currentPath) ||
							(entryPath.startsWith(currentPath + "/")
									&& config.isArchive(currentPath))) {
						match = true;
						break;
					}
				}
				if (! match)
					throw new FileNotFoundException();
			}
			
			FileHeader fh = null;
			NullOutputStream nullOut = isSolid ? new NullOutputStream() : null;
			
			for (int i = 0;; i++) {
				fh = archive.nextFileHeader();
				if (fh == null) break; // Last entry reached
				if (fh.isEncrypted() || fh.isDirectory())
					continue;
				
				String currentPath = fh.isUnicode() ? fh.getFileNameW() : fh.getFileNameString();
				currentPath = currentPath.replace('\\', '/');
				assert UtilModel.noTrailingSlash(currentPath);
				
				// TODO now: throw disk space exception
				if (entryPath.equals(currentPath)) { // Exact match
					String cacheKey = Util.joinPath(originalArchivePath, currentPath);
					File unpackedFile = unpackRarEntry(config, archive, fh, entryPath);
					return unpackCache.putIfAbsent(cacheKey, unpackedFile);
				} else if (entryPath.startsWith(currentPath + "/")
						&& config.isArchive(currentPath)) { // Partial match
					File innerArchiveFile;
					try {
						innerArchiveFile = unpackRarEntry(config, archive, fh, entryPath);
					} finally {
						archiveResource.dispose();
					}
					String cacheKey = Util.joinPath(originalArchivePath, currentPath);
					FileResource innerArchive = unpackCache.putIfAbsent(cacheKey, innerArchiveFile);
					String remainingPath = entryPath.substring(currentPath.length() + 1);
					return unpackFromArchive(config, cacheKey, innerArchive, remainingPath);
				} else if (isSolid) { // Not a match
					archive.extractFile(fh, nullOut);
				}
			}
		} catch (RarException e) {
			throw new IOException(e);
		} finally {
			Closeables.closeQuietly(archive);
			archiveResource.dispose();
		}
		throw new FileNotFoundException();
	}
	
	@NotNull
	private static File unpackRarEntry(	@NotNull IndexingConfig config,
	                                   	@NotNull Archive archive,
										@NotNull FileHeader fh,
										@NotNull String entryPath)
			throws IOException, RarException {
		String entryName = getLastPathPart(entryPath);
		OutputStream out = null;
		try {
			File unpackedFile = config.createDerivedTempFile(entryName);
			out = new FileOutputStream(unpackedFile);
			archive.extractFile(fh, out);
			return unpackedFile;
		} catch (IndexingException e) {
			throw e.getIOException();
		} finally {
			Closeables.closeQuietly(out);
		}
	}
	
	@NotNull
	private static String getLastPathPart(@NotNull String string) {
		for (int i = string.length() - 1; i >= 0; i--) {
			char c = string.charAt(i);
			if (c == '/' || c == '\\') {
				return string.substring(i + 1);
			}
		}
		return string;
	}
	
	// paths should not contain backslashes
	// entryPath is relative to archive root
	// supports HTML pairing, but has some overhead
	@NotNull
	private FileResource unpackFromSolidArchive(@NotNull IndexingConfig config,
	                                            @NotNull FileResource archiveResource,
												@NotNull final SolidArchiveTree<?> archive,
												@NotNull final String entryPath)
			throws ArchiveEncryptedException, DiskSpaceException,
			FileNotFoundException, IOException {
		final TreeNode[] matchingNode = new TreeNode[1];
		final String[] remainingPath = new String[1];
		
		try {
			new FileFolderVisitor <Exception> (archive.getArchiveFolder()) {
				protected void visitFolder(	FileFolder parent,
				                           	FileFolder folder) {
					if (! folder.isArchive()) return;
					String currentPath = archive.getArchiveEntryPath(folder);
					assert UtilModel.noTrailingSlash(currentPath);
					if (! entryPath.startsWith(currentPath + "/"))
						return; // Partial match required
					matchingNode[0] = folder;
					remainingPath[0] = entryPath.substring(currentPath.length() + 1);
					stop();
				}
				protected void visitDocument(	FileFolder parent,
				                             	FileDocument fileDocument) {
					String currentPath = archive.getArchiveEntryPath(fileDocument);
					assert UtilModel.noTrailingSlash(currentPath);
					if (! currentPath.equals(entryPath))
						return; // Exact match required
					matchingNode[0] = fileDocument;
					stop();
				}
			}.runSilently();
			
			TreeNode treeNode = matchingNode[0];
			if (treeNode == null)
				throw new FileNotFoundException();
			String cacheKey = treeNode.getPath();
			
			if (matchingNode[0] instanceof FileFolder) { // Inner archive
				try {
					archive.unpack(treeNode);
				} finally {
					archiveResource.dispose();
				}
				File innerArchiveFile = archive.getFile(treeNode);
				FileResource innerArchive = unpackCache.putIfAbsent(cacheKey, innerArchiveFile);
				return unpackFromArchive(
						config,
						treeNode.getPath(),
						innerArchive,
						remainingPath[0]
				);
			} else if (matchingNode[0] instanceof FileDocument) { // File
				FileDocument htmlDoc = (FileDocument) treeNode;
				FileFolder htmlFolder = htmlDoc.getHtmlFolder();
				if (htmlFolder == null) { // Ordinary file or HTML file without HTML folder
					archive.unpack(treeNode);
					File htmlFile = archive.getFile(treeNode);
					return unpackCache.putIfAbsent(cacheKey, htmlFile);
				} else { // HTML file with HTML folder
					List<FileDocument> docsDeep = htmlFolder.getDocumentsDeep();
					docsDeep.add(htmlDoc);
					File tempDir = Files.createTempDir();
					archive.unpack(docsDeep, tempDir);
					File htmlFile = archive.getFile(htmlDoc);
					return unpackCache.putIfAbsent(cacheKey, htmlFile, tempDir);
				}
			} else {
				throw new IllegalStateException();
			}
		} finally {
			Closeables.closeQuietly(archive);
			archiveResource.dispose();
		}
	}
	
}
