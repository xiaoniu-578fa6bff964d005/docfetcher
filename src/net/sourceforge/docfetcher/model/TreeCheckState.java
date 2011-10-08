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

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class TreeCheckState {
	
	private final Set<FolderWrapper> checkedSet = new HashSet<FolderWrapper>();
	private int folderCount = 0; // includes unchecked and checked folders
	
	void add(@NotNull Folder<?, ?> folder, boolean isChecked) {
		Util.checkNotNull(folder);
		if (isChecked)
			checkedSet.add(new FolderWrapper(folder));
		folderCount++;
	}
	
	void add(@NotNull TreeCheckState other) {
		Util.checkNotNull(other);
		checkedSet.addAll(other.checkedSet);
		folderCount += other.folderCount;
	}
	
	public boolean isChecked(@NotNull Path path) {
		Util.checkNotNull(path);
		if (checkedSet.isEmpty())
			return false;
		if (folderCount == checkedSet.size())
			return true;
		return checkedSet.contains(new PathWrapper(path));
	}
	
	private static final class PathWrapper {
		private final Path path;

		public PathWrapper(@NotNull Path path) {
			this.path = path;
		}
		
		public boolean equals(Object obj) {
			/*
			 * The hash map implementation will call this equals method rather
			 * than the one on the FolderWrapper, so we'll override this equal
			 * method and redirect to the equals method of the FolderWrapper.
			 */
			return ((FolderWrapper) obj).equals(this);
		}
		
		public int hashCode() {
			return path.getPath().hashCode();
		}
	}
	
	private static final class FolderWrapper {
		private final Folder<?, ?> folder;

		private FolderWrapper(@NotNull Folder<?, ?> folder) {
			this.folder = folder;
		}
		
		public boolean equals(Object obj) {
			String targetPath = ((PathWrapper) obj).path.getPath();
			if (targetPath.length() != getPathLength(folder))
				return false;
			PathReverseIterator it1 = new PathReverseIterator(folder);
			StringReverseIterator it2 = new StringReverseIterator(targetPath);
			while (true) {
				if (it1.hasNext()) {
					if (it2.hasNext()) {
						if (it1.next() != it2.next())
							return false;
					}
					else {
						return false;
					}
				}
				else {
					if (it2.hasNext())
						return false;
					else
						break;
				}
			}
			return true;
		}
		
		private int getPathLength(@NotNull Folder<?, ?> folder) {
			int count = 0;
			Folder<?, ?> current = folder;
			while (true) {
				Folder<?, ?> parent = current.getParent();
				if (parent == null) {
					count += current.getPath().getPath().length();
					break;
				}
				count += current.getName().length() + 1; // +1 for path separator
				current = parent;
			}
			return count;
		}
		
		public int hashCode() {
			return folder.getPathHashCode();
		}
	}
	
	private static final class PathReverseIterator {
		private enum State {
			PART, SEP, END
		}
		
		private final StringReverseIterator it = new StringReverseIterator();
		private Folder<?, ?> currentFolder;
		private State state = State.PART;

		public PathReverseIterator(@NotNull Folder<?, ?> folder) {
			setCurrentFolder(folder);
			assert it.hasNext();
		}
		
		public char next() {
			if (state == State.PART) {
				char r = it.next();
				if (!it.hasNext()) {
					Folder<?, ?> parent = currentFolder.getParent();
					if (parent == null) {
						state = State.END;
					}
					else {
						state = State.SEP;
						setCurrentFolder(parent);
					}
				}
				return r;
			}
			else if (state == State.SEP) {
				state = State.PART;
				return '/';
			}
			throw new NoSuchElementException();
		}
		
		public boolean hasNext() {
			return state != State.END;
		}
		
		private void setCurrentFolder(@NotNull Folder<?, ?> currentFolder) {
			this.currentFolder = currentFolder;
			if (currentFolder.getParent() == null)
				it.setString(currentFolder.getPath().getPath());
			else
				it.setString(currentFolder.getName());
		}
	}
	
	private static final class StringReverseIterator {
		private String string;
		private int i = -1;
		
		public StringReverseIterator() {
		}
		
		public StringReverseIterator(@NotNull String string) {
			setString(string);
		}
		
		public void setString(@NotNull String string) {
			this.string = string;
			i = string.length() - 1;
		}
		
		public char next() {
			if (i < 0)
				throw new NoSuchElementException();
			return string.charAt(i--);
		}
		
		public boolean hasNext() {
			return i >= 0;
		}
	}

}
