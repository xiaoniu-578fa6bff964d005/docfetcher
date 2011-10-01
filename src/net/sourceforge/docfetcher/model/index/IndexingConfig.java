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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.file.FileFilter;
import net.sourceforge.docfetcher.model.index.file.SolidArchiveFactory;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.sl.FsDriverLocator;

/**
 * Should not be subclassed outside the package group of this class.
 * 
 * @author Tran Nam Quang
 */
@SuppressWarnings("serial")
public class IndexingConfig implements Serializable {
	
	public static final List<String> defaultZipExtensions = Arrays.asList("zip", "jar");
	public static final List<String> defaultTextExtensions = Arrays.asList("txt", "java", "cpp", "py");
	
	public static final List<String> hiddenZipExtensions = Arrays.asList(
		"tar", "tar.gz", "tgz", "tar.bz2", "tb2", "tbz");
	
	private static final FileFilter defaultFileFilter = new FileFilter();
	private static final Pattern defaultMimePattern = Pattern.compile("");
	
	@NotNull private File rootFile;
	@Nullable private String userDirPath;
	@Nullable private File tempDir;
	@NotNull private Pattern mimePattern = defaultMimePattern;
	private boolean isPortable = AppUtil.isPortable();
	private boolean storeRelativePaths;
	@NotNull private FileFilter fileFilter = defaultFileFilter;
	private boolean htmlPairing = true;
	private boolean watchFolders = true;
	@NotNull private List<String> zipExtensions = defaultZipExtensions;
	@NotNull private List<String> textExtensions = defaultTextExtensions;
	private boolean detectExecutableArchives = false;
	private boolean indexFilenames = true;
	
	@VisibleForTesting
	public IndexingConfig() {
		this.rootFile = new File("");
	}
	
	public IndexingConfig(@NotNull File rootFile) {
		Util.checkNotNull(rootFile);
		this.rootFile = new File(getStorablePath(rootFile));
	}
	
	@NotNull
	public final File getRootFile() {
		return rootFile;
	}

	public final boolean isPortable() {
		return isPortable;
	}

	public final void setPortable(boolean isPortable) {
		this.isPortable = isPortable;
	}
	
	public final boolean isDetectExecutableArchives() {
		return detectExecutableArchives;
	}

	public final void setDetectExecutableArchives(boolean detectExecutableArchives) {
		this.detectExecutableArchives = detectExecutableArchives;
	}
	
	public final boolean isIndexFilenames() {
		return indexFilenames;
	}

	public final void setIndexFilenames(boolean indexFilenames) {
		this.indexFilenames = indexFilenames;
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

	public final boolean isStoreRelativePaths() {
		return storeRelativePaths;
	}

	public final void setStoreRelativePaths(boolean storeRelativePaths) {
		if (this.storeRelativePaths == storeRelativePaths)
			return;
		rootFile = new File(getStorablePath(rootFile, storeRelativePaths));
		this.storeRelativePaths = storeRelativePaths;
		onRootFileChanged();
	}
	
	protected void onRootFileChanged() {}

	/**
	 * For the given file, a path is returned that can be stored without
	 * breaking program portability. More specifically, this method returns
	 * either an absolute path or a path relative to the current directory,
	 * depending on the value of {@link #isStoreRelativePaths()}.
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
		return getStorablePath(file, storeRelativePaths);
	}
	
	/**
	 * @see #getStorablePath(File)
	 */
	@NotNull
	public final String getStorablePath(@NotNull File file,
										boolean storeRelativePaths) {
		String absPath = Util.getAbsPath(file);
		if (! isPortable)
			return absPath;
		assert Util.USER_DIR.isAbsolute();
		String userDirPath = getUserDirPath();
		if (absPath.equals(userDirPath))
			return "";
		if (Util.IS_WINDOWS) {
			String d1 = Util.getDriveLetter(userDirPath);
			String d2 = Util.getDriveLetter(absPath);
			if (! d1.equals(d2))
				return absPath;
		}
		if (storeRelativePaths || Util.contains(userDirPath, absPath))
			return UtilModel.getRelativePath(userDirPath, absPath);
		return absPath;
	}
	
	// throws exception if regex is malformed
	public final void setMimePattern(@NotNull String regex) {
		regex = regex.trim();
		if (regex.isEmpty())
			mimePattern = defaultMimePattern;
		else
			mimePattern = Pattern.compile(regex);
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
	public final FileFilter getFileFilter() {
		return fileFilter;
	}

	public final void setFileFilter(@Nullable FileFilter fileFilter) {
		this.fileFilter = fileFilter == null ? defaultFileFilter : fileFilter;
	}
	
	@NotNull
	public final Collection<String> getHtmlExtensions() {
		return ProgramConf.StrList.HtmlExtensions.get();
	}
	
	public final boolean isHtmlPairing() {
		return htmlPairing;
	}

	public final void setHtmlPairing(boolean htmlPairing) {
		this.htmlPairing = htmlPairing;
	}
	
	@Immutable
	@NotNull
	public final List<String> getTextExtensions() {
		return textExtensions;
	}
	
	public final void setTextExtensions(@NotNull Collection<String> textExtensions) {
		this.textExtensions = immutableUniqueLowerCase(textExtensions);
	}
	
	// Returned collection does not contain 'exe'
	@Immutable
	@NotNull
	public final List<String> getZipExtensions() {
		return zipExtensions;
	}

	public final void setZipExtensions(@NotNull Collection<String> zipExtensions) {
		this.zipExtensions = immutableUniqueLowerCase(zipExtensions);
	}
	
	@NotNull
	private List<String> immutableUniqueLowerCase(@NotNull Collection<String> strings) {
		Util.checkNotNull(strings);
		Set<String> set = Sets.newLinkedHashSet();
		for (String string : strings)
			set.add(string.toLowerCase());
		return ImmutableList.copyOf(set);
	}

	// Returned detector takes 'detect executable archives' setting into account
	@NotNull
	public final TArchiveDetector createZipDetector() {
		/*
		 * Create an extended copy of the default driver map where all
		 * user-defined extensions not known to TrueZIP are associated with the
		 * zip driver.
		 */
		final Map<FsScheme, FsDriver> driverMap = Maps.newHashMap(FsDriverLocator.SINGLETON.get());
		FsDriver zipDriver = driverMap.get(FsScheme.create("zip"));
		for (String ext : zipExtensions) {
			FsScheme scheme = FsScheme.create(ext);
			if (!driverMap.containsKey(scheme))
				driverMap.put(scheme, zipDriver);
		}
		
		FsDriverProvider driverProvider = new FsDriverProvider() {
			public Map<FsScheme, FsDriver> get() {
				return Collections.unmodifiableMap(driverMap);
			}
		};
		
		Set<String> extensions = new LinkedHashSet<String>();
		extensions.addAll(zipExtensions);
		extensions.addAll(hiddenZipExtensions);
		if (detectExecutableArchives)
			extensions.add("exe");
		return new TArchiveDetector(driverProvider, Util.join("|", extensions));
	}

	// Accepts filenames and filepaths
	// Takes 'detect executable archives' setting into account
	public final boolean isArchive(@NotNull String filename) {
		String ext = Util.getExtension(filename);
		if (detectExecutableArchives && ext.equals("exe"))
			return true;
		if (ext.equals("7z") || ext.equals("rar"))
			return true;
		if (hiddenZipExtensions.contains(ext))
			return true;
		return zipExtensions.contains(ext);
	}
	
	// Accepts filenames and filepaths
	// Takes 'detect executable archives' setting into account
	@Nullable
	public final SolidArchiveFactory getSolidArchiveFactory(@NotNull String filename) {
		/*
		 * JUnRar does not seem to support SFX RAR archives, but TrueZIP and
		 * J7Zip do support SFX Zip and SFX 7z archives, respectively.
		 */
		String ext = Util.getExtension(filename);
		if (detectExecutableArchives && ext.equals("exe"))
			return SolidArchiveFactory.SevenZip;
		if (ext.equals("7z"))
			return SolidArchiveFactory.SevenZip;
		if (ext.equals("rar"))
			return SolidArchiveFactory.Rar;
		return null;
	}
	
	// Accepts filenames and filepaths
	public final boolean isSolidArchive(@NotNull String filename) {
		return getSolidArchiveFactory(filename) != null;
	}
	
	public final boolean isWatchFolders() {
		return watchFolders;
	}
	
	public final void setWatchFolders(boolean watchFolders) {
		this.watchFolders = watchFolders;
	}

}
