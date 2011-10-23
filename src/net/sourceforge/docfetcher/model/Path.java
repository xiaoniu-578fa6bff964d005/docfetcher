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
import java.io.IOException;
import java.io.Serializable;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;

/**
 * @author Tran Nam Quang
 */
@SuppressWarnings("serial")
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
	
	private final String name;
	private final String path;
	@Nullable private transient File canonicalFile;
	
	public Path(@NotNull String path) {
		Util.checkNotNull(path);
		this.path = normalizePath(path);
		this.canonicalFile = getCanonicalFile();
		this.name = canonicalFile.getName();
	}
	
	@NotNull
	private static String normalizePath(@NotNull String path) {
		return Util.fileSepMatcher.trimTrailingFrom(path).replace("\\", "/");
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
			try {
				canonicalFile = new File(path).getCanonicalFile();
			}
			catch (IOException e) {
				Util.printErr(e);
				canonicalFile = new File(path).getAbsoluteFile();
			}
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
