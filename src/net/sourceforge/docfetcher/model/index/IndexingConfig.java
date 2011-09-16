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

package net.sourceforge.docfetcher.model.index;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.file.FileFilter;
import net.sourceforge.docfetcher.model.index.file.SolidArchiveFactory;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import de.schlichtherle.truezip.file.TArchiveDetector;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
@SuppressWarnings("serial")
public class IndexingConfig implements Serializable {
	
	private static final Collection<String> defaultZipExtensions = Arrays.asList("zip", "jar", "exe"); // TODO now: add more zip extensions
	private static final Collection<String> defaultTextExtensions = Arrays.asList("txt", "java", "cpp", "py");
	private static final FileFilter defaultFileFilter = new FileFilter();
	private static final Pattern defaultMimePattern = Pattern.compile("");
	
	@Nullable private String userDirPath;
	@Nullable private Boolean isWindows;
	@Nullable private File tempDir;
	@NotNull private Pattern mimePattern = defaultMimePattern;
	private boolean isPortable = AppUtil.isPortable();
	private boolean useRelativePaths;
	@NotNull private FileFilter fileFilter = defaultFileFilter;
	private boolean htmlPairing = true;
	private boolean watchFolders = true;
	@NotNull private Collection<String> textExtensions = defaultTextExtensions;
	@NotNull private Collection<String> zipExtensions = defaultZipExtensions;

	public final boolean isPortable() {
		return isPortable;
	}

	public final void setPortable(boolean isPortable) {
		this.isPortable = isPortable;
	}

	@NotNull
	public final String getUserDirPath() {
		if (userDirPath == null)
			return Util.USER_DIR_PATH;
		return userDirPath;
	}

	public final void setUserDirPath(@Nullable String userDirPath) {
		if (userDirPath == null)
			this.userDirPath = null;
		else
			this.userDirPath = Util.getAbsPath(userDirPath);
	}

	public final boolean isWindows() {
		if (isWindows == null)
			return Util.IS_WINDOWS;
		return isWindows;
	}

	/**
	 * Sets whether the indexing methods should assume they are running on
	 * Windows. If the argument is null, Windows will be detected automatically.
	 */
	public final void setWindows(@Nullable Boolean isWindows) {
		this.isWindows = isWindows;
	}

	@NotNull
	public final File getTempDir() {
		if (tempDir != null && tempDir.isDirectory() && tempDir.canWrite())
			return tempDir;
		return Util.TEMP_DIR;
	}

	/**
	 * Sets the temporary directory to be used during indexing. The directory
	 * specified by the given path will only be used if it represents an
	 * existing, writable directory.
	 */
	public final void setTempDir(@Nullable File tempDir) {
		this.tempDir = tempDir;
	}

	public final boolean isUseRelativePaths() {
		return useRelativePaths;
	}

	public final void setUseRelativePaths(boolean useRelativePaths) {
		this.useRelativePaths = useRelativePaths;
	}

	/**
	 * For the given file, a path is returned that can be stored without
	 * breaking program portability. More specifically, this method returns
	 * either an absolute path or a path relative to the current directory,
	 * depending on the value of {@link #isUseRelativePaths()}.
	 * <p>
	 * There are some exceptions to this rule:
	 * <ul>
	 * <li>If the program is not portable, this method returns an absolute path.
	 * <li>If the program is portable and if the given file is equal to or
	 * inside the current directory, this method returns a relative path.
	 * <li>If the file and the current directory reside on different drives
	 * (e.g. "C:\" and "D:\"), this method returns an absolute path. This
	 * applies to Windows only.
	 * </ul>
	 * <p>
	 * The separators are always forward slashes (i.e., "/"). This does not
	 * affect portability since forward slashes are valid path separators on
	 * both Windows and Linux.
	 */
	@NotNull
	public final String getStorablePath(@NotNull File file) {
		String absPath = Util.getAbsPath(file);
		if (! isPortable)
			return absPath;
		assert Util.USER_DIR.isAbsolute();
		String userDirPath = getUserDirPath();
		if (absPath.equals(userDirPath))
			return "";
		if (isWindows()) {
			String d1 = UtilModel.getDriveLetter(userDirPath);
			String d2 = UtilModel.getDriveLetter(absPath);
			if (! d1.equals(d2))
				return absPath;
		}
		if (useRelativePaths || Util.contains(userDirPath, absPath))
			return UtilModel.getRelativePath(userDirPath, absPath);
		return absPath;
	}
	
	// throws exception if regex is malformed
	public final void setMimePattern(@Nullable String regex) {
		mimePattern = regex == null ? defaultMimePattern : Pattern.compile(regex);
	}
	
	public final boolean matchesMimePattern(@NotNull String filename) {
		return mimePattern.matcher(filename).matches();
	}
	
	@NotNull
	public final File createDerivedTempFile(@NotNull String filename)
			throws IndexingException {
		try {
			return Util.createDerivedTempFile(filename, getTempDir());
		} catch (IOException e) {
			throw new IndexingException(e);
		}
	}
	
	// Fail if not enough disk space for extraction
	public final void checkDiskSpaceInTempDir(long requiredSpace)
			throws DiskSpaceException {
		File customTempDir = getTempDir();
		long usableSpace = customTempDir.getUsableSpace();
		if (requiredSpace > usableSpace) {
			String msg = "Not enough disk space on '%s' " +
					"to unpack archive entries. " +
					"Available: %s MB. Needed: %s MB.";
			msg = String.format(msg,
					Util.getSystemAbsPath(customTempDir),
					toMegabyteString(usableSpace),
					toMegabyteString(requiredSpace)
			);
			throw new DiskSpaceException(msg);
		}
	}

	private static String toMegabyteString(Long bytes) {
		double megabytes = (double) bytes / (1024 * 1024);
		return String.format("%.1f", megabytes);
	}
	
	@NotNull
	public FileFilter getFileFilter() {
		return fileFilter;
	}

	public void setFileFilter(@Nullable FileFilter fileFilter) {
		this.fileFilter = fileFilter == null ? defaultFileFilter : fileFilter;
	}
	
	@NotNull
	public Collection<String> getHtmlExtensions() {
		return ProgramConf.StrList.HtmlExtensions.get();
	}
	
	public boolean isHtmlPairing() {
		return htmlPairing;
	}

	public void setHtmlPairing(boolean htmlPairing) {
		this.htmlPairing = htmlPairing;
	}
	
	@NotNull
	public Collection<String> getTextExtensions() {
		return textExtensions;
	}
	
	public void setTextExtensions(@Nullable Collection<String> textExtensions) {
		this.textExtensions = textExtensions == null ? defaultTextExtensions : textExtensions;
	}
	
	public void setZipExtensions(@Nullable Collection<String> zipExtensions) {
		this.zipExtensions = zipExtensions == null ? defaultZipExtensions : zipExtensions;
	}

	@NotNull
	public TArchiveDetector createZipDetector() {
		return new TArchiveDetector(Util.join("|", zipExtensions));
	}

	// Accepts filenames and filepaths
	public boolean isArchive(@NotNull String filename) {
		String ext = Util.getExtension(filename);
		if (ext.equals("7z") || ext.equals("rar") || ext.equals("exe"))
			return true;
		return zipExtensions.contains(ext);
	}
	
	// Accepts filenames and filepaths
	@Nullable
	public SolidArchiveFactory getSolidArchiveFactory(@NotNull String filename) {
		/*
		 * JUnRar does not seem to support SFX RAR archives, but TrueZIP and
		 * J7Zip do support SFX Zip and SFX 7z archives, respectively.
		 */
		String ext = Util.getExtension(filename);
		if (ext.equals("7z") || ext.equals("exe"))
			return SolidArchiveFactory.SevenZip;
		if (ext.equals("rar"))
			return SolidArchiveFactory.Rar;
		return null;
	}
	
	// Accepts filenames and filepaths
	public boolean isSolidArchive(@NotNull String filename) {
		return getSolidArchiveFactory(filename) != null;
	}
	
	public boolean isWatchFolders() {
		return watchFolders;
	}
	
	public void setWatchFolders(boolean watchFolders) {
		this.watchFolders = watchFolders;
	}

}
