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
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.ImmutableCopy;
import net.sourceforge.docfetcher.base.annotations.MutableCopy;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;

import com.google.common.collect.Maps;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
@SuppressWarnings("serial")
public abstract class Folder
	<D extends Document<D, F>, F extends Folder<D, F>>
		extends TreeNode implements ViewNode {
	
	public interface Predicate<T> {
		boolean matches(T candidate);
	}
	
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
	
	// Making these events non-static would lead to trouble with serialization
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
	@Nullable private HashMap<String, F> subFolders;
	@Nullable private F parent;
	private final String path;
	
	/*
	 * This field is marked as volatile because it will be accessed from another
	 * thread during search.
	 */
	private volatile boolean isChecked = true;
	
	public Folder(@NotNull String name, @NotNull String path) {
		super(name, name);
		this.path = UtilModel.normalizePath(path);
	}
	
	@Nullable
	public synchronized F getParent() {
		return parent;
	}
	
	@Nullable
	@RecursiveMethod
	@SuppressWarnings("unchecked")
	public synchronized F getRoot() {
		return parent == null ? (F) this : parent.getRoot();
	}

	@NotNull
	public final String getPath() {
		return path;
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
	public synchronized final void putSubFolder(@NotNull F folder) {
		Util.checkThat(folder.path.startsWith(path + "/"));
		if (subFolders == null)
			subFolders = Maps.newHashMap();
		if (folder.parent != null)
			folder.parent.subFolders.remove(folder);
		folder.parent = (F) this;
		subFolders.put(folder.getName(), folder);
		evtFolderAdded.fire(new FolderEvent(this, folder));
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
	
	public synchronized final void removeChildren() {
		if (documents != null) {
			for (D doc : documents.values())
				doc.parent = null;
			documents.clear();
			documents = null;
		}
		if (subFolders != null) {
			Collection<F> toNotify = subFolders.values();
			subFolders = null;
			for (F subFolder : toNotify)
				subFolder.parent = null;
			for (F subFolder : toNotify)
				evtFolderRemoved.fire(new FolderEvent(this, subFolder));
		}
	}
	
	/**
	 * Removes the given subfolder from the receiver. Does nothing if the given
	 * subfolder is null.
	 */
	public synchronized final void removeSubFolder(@Nullable F subFolder) {
		if (subFolders == null || subFolder == null) return;
		F candidate = subFolders.remove(subFolder.getName());
		Util.checkThat(candidate == subFolder);
		subFolder.parent = null;
		if (subFolders.isEmpty())
			subFolders = null;
		evtFolderRemoved.fire(new FolderEvent(this, subFolder));
	}
	
	public synchronized final void removeDocuments(@NotNull Predicate<D> predicate) {
		if (documents == null) return;
		Iterator<D> docIt = documents.values().iterator();
		while (docIt.hasNext()) {
			D doc = docIt.next();
			if (predicate.matches(doc)) {
				docIt.remove();
				doc.parent = null;
			}
		}
		if (documents.isEmpty())
			documents = null;
	}
	
	public synchronized final void removeSubFolders(@NotNull Predicate<F> predicate) {
		if (subFolders == null) return;
		Iterator<F> subFolderIt = subFolders.values().iterator();
		List<F> toNotify = new ArrayList<F>(subFolders.size());
		while (subFolderIt.hasNext()) {
			F subFolder = subFolderIt.next();
			if (predicate.matches(subFolder)) {
				subFolderIt.remove();
				subFolder.parent = null;
				toNotify.add(subFolder);
			}
		}
		if (subFolders.isEmpty())
			subFolders = null;
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
		if (subFolders == null) return null;
		return subFolders.get(name);
	}
	
	public synchronized final int getSubFolderCount() {
		if (subFolders == null) return 0;
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
	
	public final boolean isChecked() {
		return isChecked;
	}
	
	public final void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
	
	@NotNull
	@SuppressWarnings("unchecked")
	public final TreeCheckState getTreeCheckState() {
		final TreeCheckState state = new TreeCheckState();
		new FolderVisitor<D, F, Throwable>((F) this) {
			protected void visitFolder(F parent, F folder) {
				if (folder.isChecked)
					state.checkedPaths.add(folder.getPath());
				state.folderCount++;
			}
		}.runSilently();
		return state;
	}
	
	public final boolean isIndex() {
		return false;
	}
	
	@ImmutableCopy
	@NotNull
	public final List<String> getDocumentIds() {
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
	 * Returns the document underneath the receiver that has the given path, or
	 * null if there is no such document. The search for the document is done
	 * recursively. Trailing slashes in the given path will be ignored, and
	 * backward slashes are automatically converted to forward slashes.
	 * <p>
	 * This method does not convert between absolute and relative paths, so if
	 * the documents of the receiver have relative paths, the given path must
	 * also be a relative path.
	 */
	@Nullable
	public final D findDocument(@NotNull String targetPath) {
		Util.checkNotNull(targetPath);
		targetPath = UtilModel.normalizePath(targetPath);
		return findDocumentUnchecked(targetPath);
	}
	
	/**
	 * Recursive helper method for {@link #findDocument(String)}.
	 */
	@Nullable
	@RecursiveMethod
	private D findDocumentUnchecked(@NotNull String targetPath) {
		if (documents != null) {
			for (D document : documents.values()) {
				String path = document.getPath();
				assert UtilModel.noTrailingSlash(path);
				if (targetPath.equals(path))
					return document;
			}
		}
		if (subFolders != null) {
			for (F subFolder : subFolders.values()) {
				String path = subFolder.getPath();
				assert UtilModel.noTrailingSlash(path);
				if (targetPath.startsWith(path + "/"))
					return subFolder.findDocumentUnchecked(targetPath);
			}
		}
		return null;
	}
	
}
