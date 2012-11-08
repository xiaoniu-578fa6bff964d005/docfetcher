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

package net.sourceforge.docfetcher.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.ImmutableCopy;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public abstract class Folder
	<D extends Document<D, F>, F extends Folder<D, F>>
		extends TreeNode implements ViewNode {

	public static final class FolderEvent {
		public final Folder<?, ?> parent;
		public final Folder<?, ?> folder;

		@VisibleForPackageGroup
		public FolderEvent(	@NotNull Folder<?, ?> parent,
							@NotNull Folder<?, ?> folder) {
			Util.checkNotNull(parent, folder);
			this.parent = parent;
			this.folder = folder;
		}
	}

	/*
	 * TODO post-release-1.1: Rethink the synchronization used here. Maybe use
	 * a global static lock for all instances of TreeNode, Folder, Document, etc.?
	 */
	
	private static final long serialVersionUID = 1L;

	// Making these events non-static would lead to trouble with serialization
	public static final Event<FolderEvent> evtFolderAdding = new Event<FolderEvent>();
	public static final Event<FolderEvent> evtFolderAdded = new Event<FolderEvent>();
	public static final Event<FolderEvent> evtFolderRemoved = new Event<FolderEvent>();

	/*
	 * The children of instances of this class are stored as maps for the
	 * following reasons:
	 *
	 * (1) Running an index update involves computing a tree diff, which
	 * requires quick access to the children using a string-valued identifier
	 * (e.g. filename).
	 *
	 * (2) It prevents insertion of duplicate identifiers. (However, using a map
	 * doesn't prevent the situation that a document and a subfolder are stored
	 * with the same identifier, since documents and subfolders are stored in
	 * different maps.)
	 *
	 * These maps are set to null when they're empty in order to avoid wasting
	 * RAM when the tree is very large and has many empty leaf nodes.
	 */
	@Nullable private HashMap<String, D> documents;
	@Nullable protected HashMap<String, F> subFolders;

	/*
	 * If this is a root folder, then it has a non-null path and a null parent.
	 * For non-root folders, it's the exact opposite, i.e. they have a null path
	 * and a non-null parent. HTML folders and SolidArchiveTree roots are
	 * treated as root folders.
	 */
	@Nullable protected F parent;
	@Nullable protected Path path;
	private int pathHashCode;

	/**
	 * The last time this object was modified. Null if the object has no last
	 * modified field (e.g. regular folder).
	 */
	@Nullable private Long lastModified;

	protected boolean isChecked = true;

	@SuppressWarnings("unchecked")
	protected Folder(	@NotNull F parent,
						@NotNull String name,
						@Nullable Long lastModified) {
		super(name);
		Util.checkNotNull(parent);
		this.parent = parent;
		this.lastModified = lastModified;
		parent.putSubFolder((F) this);
		updatePathHashCode();
	}

	protected Folder(@NotNull Path path, @Nullable Long lastModified) {
		super(path.getName());
		this.path = path;
		this.lastModified = lastModified;
		updatePathHashCode();
	}
	
	public final synchronized int getParentCount() {
		int count = 0;
		F current = parent;
		while (current != null) {
			count++;
			current = current.parent; // this is not really thread-safe :-/
		}
		return count;
	}

	protected void updatePathHashCode() {
		if (path != null) {
			pathHashCode = path.getPath().hashCode();
		}
		else {
			String parentPath = parent.getPath().getPath();
			String thisPath = Util.joinPath(parentPath, getName());
			pathHashCode = thisPath.hashCode();
		}
	}

	synchronized int getPathHashCode() {
		return pathHashCode;
	}

	@Nullable
	public synchronized final F getParent() {
		return parent;
	}

	@Nullable
	@RecursiveMethod
	@SuppressWarnings("unchecked")
	public synchronized final F getRoot() {
		return parent == null ? (F) this : parent.getRoot();
	}

	// TODO post-release-1.1: This method is somewhat expensive. Try to avoid calling it
	@SuppressWarnings("unchecked")
	@NotNull
	public synchronized final Path getPath() {
		assert (parent == null) == (path != null);

		// Just return the path if this instance is a root
		if (path != null)
			return path;

		// Create a list of nodes from the root down to this instance
		LinkedList<F> list = new LinkedList<F>();
		list.add((F) this);
		F current = parent;
		while (current != null) {
			list.addFirst(current);
			current = current.parent;
		}

		// Construct path for this instance
		StringBuilder sb = new StringBuilder();
		sb.append(list.removeFirst().path.getPath()); // root path
		for (F node : list) {
			sb.append("/");
			sb.append(node.getName());
		}

		return new Path(sb.toString());
	}

	synchronized final void setPath(@NotNull Path path) {
		Util.checkNotNull(path);
		this.path = path;
		parent = null;
		updatePathHashCode();
	}

	@Nullable
	public synchronized final Long getLastModified() {
		return lastModified;
	}

	public synchronized final void setLastModified(@Nullable Long lastModified) {
		this.lastModified = lastModified;
	}

	// will replace document with identical name;
	// will detach document from previous parent if there is one
	@SuppressWarnings("unchecked")
	public synchronized final void putDocument(@NotNull D doc) {
		if (documents == null)
			documents = Maps.newHashMap();
		documents.put(doc.getName(), doc);
		if (doc.parent != null && doc.parent != this)
			doc.parent.removeDocument(doc);
		doc.parent = (F) this;
	}

	// will replace folder with identical name
	@SuppressWarnings("unchecked")
	public final void putSubFolder(@NotNull F subFolder) {
		evtFolderAdding.fire(new FolderEvent(this, subFolder));
		synchronized (this) {
			if (subFolders == null)
				subFolders = Maps.newHashMap();
			if (subFolder.parent != null)
				subFolder.parent.subFolders.remove(subFolder);
			subFolder.parent = (F) this;
			subFolder.path = null;
			subFolder.updatePathHashCode();
			subFolders.put(subFolder.getName(), subFolder);
		}
		evtFolderAdded.fire(new FolderEvent(this, subFolder));
	}

	/**
	 * Removes the given document from the receiver. Does nothing if the given
	 * document is null.
	 */
	public synchronized final void removeDocument(@Nullable D doc) {
		if (documents == null || doc == null) return;
		D candidate = documents.remove(doc.getName());
		Util.checkThat(candidate == doc);
		doc.parent = null;
		if (documents.isEmpty())
			documents = null;
	}

	public final void removeChildren() {
		Collection<F> toNotify = subFolders == null
			? Collections.<F>emptyList()
			: subFolders.values();
		synchronized (this) {
			if (documents != null) {
				for (D doc : documents.values())
					doc.parent = null;
				documents.clear();
				documents = null;
			}
			if (subFolders != null) {
				subFolders = null;
				for (F subFolder : toNotify) {
					subFolder.path = subFolder.getPath();
					subFolder.parent = null;
				}
			}
		}
		for (F subFolder : toNotify)
			evtFolderRemoved.fire(new FolderEvent(this, subFolder));
	}

	/**
	 * Removes the given subfolder from the receiver. Does nothing if the given
	 * subfolder is null. After it is removed, it will still have a valid path
	 * that can be obtained via {@link #getPath()}.
	 */
	public final void removeSubFolder(@Nullable F subFolder) {
		if (subFolder == null)
			return;
		synchronized (this) {
			if (subFolders == null)
				return;
			F candidate = subFolders.remove(subFolder.getName());
			Util.checkThat(candidate == subFolder);

			/*
			 * Folder instances must always either have a parent or a path, so we'll
			 * reconstruct the subfolder's path and set it before we nullify the
			 * parent.
			 */
			subFolder.path = subFolder.getPath();
			subFolder.parent = null;

			if (subFolders.isEmpty())
				subFolders = null;
		}
		evtFolderRemoved.fire(new FolderEvent(this, subFolder));
	}

	public synchronized final void removeDocuments(@NotNull Predicate<D> predicate) {
		if (documents == null) return;
		Iterator<D> docIt = documents.values().iterator();
		while (docIt.hasNext()) {
			D doc = docIt.next();
			if (predicate.apply(doc)) {
				docIt.remove();
				doc.parent = null;
			}
		}
		if (documents.isEmpty())
			documents = null;
	}

	/**
	 * Removes all subfolders from the receiver that satisfy the given
	 * predicate. The removed subfolders will still have valid paths that can be
	 * obtained via {@link #getPath()}.
	 */
	public synchronized final void removeSubFolders(@NotNull Predicate<F> predicate) {
		List<F> toNotify = new ArrayList<F>(subFolders == null ? 0 : subFolders.size());
		synchronized (this) {
			if (subFolders == null) return;
			Iterator<F> subFolderIt = subFolders.values().iterator();
			while (subFolderIt.hasNext()) {
				F subFolder = subFolderIt.next();
				if (predicate.apply(subFolder)) {
					subFolderIt.remove();
					subFolder.path = subFolder.getPath();
					subFolder.parent = null;
					toNotify.add(subFolder);
				}
			}
			if (subFolders.isEmpty())
				subFolders = null;
		}
		for (F subFolder : toNotify)
			evtFolderRemoved.fire(new FolderEvent(this, subFolder));
	}

	@Nullable
	public synchronized final D getDocument(String name) {
		if (documents == null) return null;
		return documents.get(name);
	}

	public synchronized final int getDocumentCount() {
		if (documents == null) return 0;
		return documents.size();
	}

	@ImmutableCopy
	@NotNull
	public synchronized final List<D> getDocuments() {
		return UtilModel.nullSafeImmutableList(documents);
	}

	@ImmutableCopy
	@NotNull
	public synchronized final Map<String, D> getDocumentMap() {
		return UtilModel.nullSafeImmutableMap(documents);
	}

	@MutableCopy
	@NotNull
	@SuppressWarnings("unchecked")
	public synchronized final List<D> getDocumentsDeep() {
		final List<D> docsDeep = new ArrayList<D> ();
		new FolderVisitor<D, F, Throwable>((F) this) {
			protected void visitDocument(F parent, D fileDocument) {
				docsDeep.add(fileDocument);
			}
		}.runSilently();
		return docsDeep;
	}

	@Nullable
	public synchronized final F getSubFolder(String name) {
		if (subFolders == null)
			return null;
		return subFolders.get(name);
	}

	public synchronized final int getSubFolderCount() {
		if (subFolders == null)
			return 0;
		return subFolders.size();
	}

	@ImmutableCopy
	@NotNull
	public synchronized final List<F> getSubFolders() {
		return UtilModel.nullSafeImmutableList(subFolders);
	}

	@ImmutableCopy
	@NotNull
	public synchronized final Map<String, F> getSubFolderMap() {
		return UtilModel.nullSafeImmutableMap(subFolders);
	}

	public synchronized final int getChildCount() {
		int count = 0;
		if (documents != null)
			count += documents.size();
		if (subFolders != null)
			count += subFolders.size();
		return count;
	}

	@ImmutableCopy
	@NotNull
	public synchronized final Iterable<ViewNode> getChildren() {
		Collection<F> col = getSubFolders(); // returns a copy
		return UtilGlobal.<ViewNode>convert(col);
	}

	public synchronized final boolean isChecked() {
		return isChecked;
	}

	public synchronized final void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}

	@NotNull
	@SuppressWarnings("unchecked")
	public synchronized final TreeCheckState getTreeCheckState() {
		final TreeCheckState state = new TreeCheckState();
		state.add(this, isChecked);
		new FolderVisitor<D, F, Throwable>((F) this) {
			protected void visitFolder(F parent, F folder) {
				state.add(folder, folder.isChecked);
			}
		}.runSilently();
		return state;
	}

	public final boolean isIndex() {
		return false;
	}

	@ImmutableCopy
	@NotNull
	public synchronized final List<String> getDocumentIds() {
		if (documents == null)
			return Collections.emptyList();
		String[] uids = new String[documents.size()];
		int i = 0;
		for (D document : documents.values()) {
			uids[i] = document.getUniqueId();
			i++;
		}
		return Arrays.asList(uids);
	}

	/**
	 * Returns the tree node underneath the receiver that has the given path, or
	 * null if there is no such tree node. The search for the tree node is done
	 * recursively. Trailing slashes in the given path will be ignored, and
	 * backward slashes are automatically converted to forward slashes.
	 * <p>
	 * This method does not convert between absolute and relative paths, so if
	 * the tree nodes of the receiver have relative paths, the given path must
	 * also be a relative path.
	 */
	@Nullable
	@ThreadSafe
	public final TreeNode findTreeNode(@NotNull Path targetPath) {
		Util.checkNotNull(targetPath);
		if (getPath().equals(targetPath))
			return this;
		return findTreeNodeUnchecked(targetPath);
	}

	/**
	 * Recursive helper method for {@link #findTreeNode(String)}.
	 */
	@Nullable
	@RecursiveMethod
	@ThreadSafe
	protected synchronized TreeNode findTreeNodeUnchecked(@NotNull Path targetPath) {
		/*
		 * TODO post-release-1.1: since getPath() constructs the returned path
		 * dynamically, this search algorithm is somewhat inefficient. Maybe
		 * improve it? (Consider making use of the path hashcode.)
		 */
		if (documents != null) {
			for (D document : documents.values()) {
				Path path = document.getPath();
				if (targetPath.equals(path))
					return document;
			}
		}
		if (subFolders != null) {
			for (F subFolder : subFolders.values()) {
				Path path = subFolder.getPath();
				if (targetPath.equals(path))
					return subFolder;
				if (path.contains(targetPath))
					return subFolder.findTreeNodeUnchecked(targetPath);
			}
		}
		return null;
	}

	public synchronized final boolean hasErrorsDeep() {
		if (hasErrors())
			return true;
		if (documents != null)
			for (D document : documents.values())
				if (document.hasErrors())
					return true;
		if (subFolders != null)
			for (F subFolder : subFolders.values())
				if (subFolder.hasErrorsDeep())
					return true;
		return false;
	}

}
