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

package net.sourceforge.docfetcher.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.Normalizer;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

import org.aspectj.lang.annotation.SuppressAjWarnings;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;

/**
 * @author Tran Nam Quang
 */
public final class Path implements Serializable {
	
	public static final class PathParts {
		private final Path left;
		private final String right;
		
		private PathParts(@NotNull Path left, @NotNull String right) {
			this.left = left;
			this.right = right;
		}
		@NotNull
		public Path getLeft() {
			return left;
		}
		@NotNull
		public String getRight() {
			return right;
		}
	}
	
	private static final class FilePredicate implements Predicate<Path> {
		private static final FilePredicate instance = new FilePredicate();
		
		public boolean apply(Path path) {
			return path.getCanonicalFile().isFile();
		}
	}
	
	private static final long serialVersionUID = 1L;
	
	private final String name;
	private final String path;
	@Nullable private transient File canonicalFile;
	
	public Path(@NotNull File file) {
		this(file.getPath());
	}
	
	public Path(@NotNull String path) {
		Util.checkNotNull(path);
		this.path = normalizePath(path);
		this.canonicalFile = getCanonicalFile();
		
		/*
		 * If the file is a root, such as '/' or 'C:', then File.getName() will
		 * return an empty string. In that case, use toString() instead, which
		 * will return '/' and 'C:', respectively. We don't want an empty string
		 * as name because it will be used for display on the GUI.
		 */
		this.name = getDisplayName(canonicalFile);
	}
	
	@NotNull
	@SuppressAjWarnings
	private static String getDisplayName(@NotNull File canonicalFile) {
		return canonicalFile.getParent() == null
			? canonicalFile.toString()
			: canonicalFile.getName();
	}
	
	@NotNull
	private static String normalizePath(@NotNull String path) {
		path = Util.fileSepMatcher.trimTrailingFrom(path).replace("\\", "/");
		return normalizeUnicode(path, true);
	}
	
	@NotNull
	private static String normalizeUnicode(@NotNull String str, boolean composed) {
	    Normalizer.Form form = composed ? Normalizer.Form.NFC : Normalizer.Form.NFD;
	    if (!Normalizer.isNormalized(str, form))
			return Normalizer.normalize(str, form);
	    return str;
	}
	
	@NotNull
	public String getName() {
		return name;
	}
	
	@NotNull
	public String getPath() {
		return path;
	}
	
	@NotNull
	public Path createSubPath(@NotNull String pathPart) {
		return new Path(Util.joinPath(path, pathPart));
	}
	
	public boolean contains(@NotNull Path subPath) {
		return subPath.getCanonicalPath().startsWith(getCanonicalPath() + Util.FS);
	}
	
	@NotNull
	public File getCanonicalFile() {
		if (canonicalFile == null) {
			/*
			 * On Mac OS X, return a file with decomposed path. On all other
			 * platforms, return a file with composed path.
			 */
			String path1 = normalizeUnicode(path, !Util.IS_MAC_OS_X);
			canonicalFile = Util.getCanonicalFile(path1);
		}
		return canonicalFile;
	}
	
	@NotNull
	public String getCanonicalPath() {
		return getCanonicalFile().getPath();
	}
	
	@NotNull
	public String toString() {
		return path;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Path))
			return false;
		return Objects.equal(path, ((Path)obj).path);
	}
	
	public int hashCode() {
		return path.hashCode();
	}
	
	@Nullable
	public PathParts splitFromRight(@NotNull Predicate<Path> leftPredicate) {
		return splitFromRight(this, "", leftPredicate);
	}
	
	// Splits the receiver's path from the right until the left part corresponds to an existing file
	// Does not check for existing directories
	@NotNull
	public PathParts splitAtExistingFile() throws FileNotFoundException {
		PathParts parts = splitFromRight(this, "", FilePredicate.instance);
		if (parts == null)
			throw new FileNotFoundException();
		return parts;
	}

	@Nullable
	@RecursiveMethod
	private static PathParts splitFromRight(@NotNull Path left,
											@NotNull String right,
											@NotNull Predicate<Path> leftPredicate) {
		if (leftPredicate.apply(left))
			return new PathParts(left, right);
		
		// Reached left end of a relative path
		if (!left.getPath().contains("/") && !left.getPath().contains("\\"))
			return null;
		
		String[] leftParts = splitAtLastSeparator(left.getPath());
		
		// Reached Unix root
		if (leftParts[0].isEmpty())
			return null;
		
		// Reached Windows root
		if (leftParts[0].matches("[a-zA-Z]:"))
			return null;
		
		// Move by one path part to the left and recurse
		String newRight = Util.joinPath(leftParts[1], right);
		return splitFromRight(new Path(leftParts[0]), newRight, leftPredicate);
	}
	
	@NotNull
	public PathParts splitAtLastSeparator() {
		String[] parts = splitAtLastSeparator(path);
		return new PathParts(new Path(parts[0]), parts[1]);
	}
	
	/**
	 * Splits the given path string at the last path separator character (either
	 * forward or backward slash). If the given string does not contain path
	 * separators, the returned array contains the given string and an empty
	 * string.
	 */
	@NotNull
	private static String[] splitAtLastSeparator(@NotNull String string) {
		for (int i = string.length() - 1; i >= 0; i--) {
			char c = string.charAt(i);
			if (c == '/' || c == '\\') {
				return new String[] {
					string.substring(0, i),
					string.substring(i + 1)
				};
			}
		}
		return new String[] {string, ""};
	}
	
}
