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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.Immutable;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;

import com.google.common.collect.Maps;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public abstract class Folder
	<D extends Document<D, F>, F extends Folder<D, F>>
		extends TreeNode implements ViewNode {
	
	private static final long serialVersionUID = -6239935045535752193L;

	public interface Predicate<T> {
		boolean matches(T candidate);
	}
	
	/*
	 * The children are stored as maps for several reasons:
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
	 * (3) Filtering the search results by location requires fast
	 * identifier-based traversal of the tree in order to determine whether a
	 * particular result item is checked in the tree viewer or not. TODO doc:
	 * still relevant?
	 * 
	 * These maps are set to null when they're empty in order to avoid wasting
	 * RAM when the tree is very large and has many empty leaf nodes.
	 */
	@Nullable protected HashMap<String, D> documents;
	
	/**
	 * This field is only marked public because of limitations of Java's
	 * visibility rules. Do not access this field directly, unless you know what
	 * you are doing.
	 */
	@Nullable public HashMap<String, F> subFolders;
	
	@NotNull private final String path;
	
	/*
	 * This field is marked as volatile because it will be accessed from another
	 * thread during search.
	 */
	private volatile boolean isChecked = true;
	
	public Folder(@NotNull String name, @NotNull String path) {
		super(name, name);
		this.path = Util.checkNotNull(path).replace('\\', '/');
	}

	@NotNull
	public final String getPath() {
		return path;
	}
	
	// will replace document with identical name;
	// will detach document from previous parent if there is one
	@SuppressWarnings("unchecked")
	public final void putDocument(@NotNull D doc) {
		if (documents == null)
			documents = Maps.newHashMap();
		documents.put(doc.getName(), doc);
		if (doc.parent != null && doc.parent != this)
			doc.parent.removeDocument(doc);
		doc.parent = (F) this;
	}
	
	// will replace folder with identical name
	public final void putSubFolder(@NotNull F folder) {
		if (subFolders == null)
			subFolders = Maps.newHashMap();
		subFolders.put(folder.getName(), folder);
	}
	
	/**
	 * Removes the given document from the receiver. Does nothing if the given
	 * document is null.
	 */
	public final void removeDocument(@Nullable D doc) {
		if (documents == null || doc == null) return;
		D candidate = documents.remove(doc.getName());
		Util.checkThat(candidate == doc);
		doc.parent = null;
		if (documents.isEmpty())
			documents = null;
	}
	
	public final void removeChildren() {
		if (documents != null) {
			for (D doc : documents.values())
				doc.parent = null;
			documents.clear();
			documents = null;
		}
		if (subFolders != null) {
			subFolders.clear();
			subFolders = null;
		}
	}
	
	/**
	 * Removes the given subfolder from the receiver. Does nothing if the given
	 * subfolder is null.
	 */
	public final void removeSubFolder(@Nullable F subFolder) {
		if (subFolders == null || subFolder == null) return;
		F candidate = subFolders.remove(subFolder.getName());
		Util.checkThat(candidate == subFolder);
		if (subFolders.isEmpty())
			subFolders = null;
	}
	
	public final void removeDocuments(@NotNull Predicate<D> predicate) {
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
	
	public final void removeSubFolders(@NotNull Predicate<F> predicate) {
		if (subFolders == null) return;
		Iterator<F> subFolderIt = subFolders.values().iterator();
		while (subFolderIt.hasNext()) {
			F subFolder = subFolderIt.next();
			if (predicate.matches(subFolder))
				subFolderIt.remove();
		}
		if (subFolders.isEmpty())
			subFolders = null;
	}
	
	@Nullable
	public final D getDocument(String name) {
		if (documents == null) return null;
		return documents.get(name);
	}
	
	public final int getDocumentCount() {
		if (documents == null) return 0;
		return documents.size();
	}
	
	@Immutable
	@NotNull
	public final Collection<D> getDocuments() {
		return UtilModel.nullSafeImmutableCol(documents);
	}
	
	@Immutable
	@NotNull
	public final Map<String, D> getDocumentMap() {
		return UtilModel.nullSafeImmutableMap(documents);
	}
	
	@NotNull
	@SuppressWarnings("unchecked")
	public final List<D> getDocumentsDeep() {
		final List<D> docsDeep = new ArrayList<D> ();
		new FolderVisitor<D, F, Throwable>((F) this) {
			protected void visitDocument(F parent, D fileDocument) {
				docsDeep.add(fileDocument);
			}
		}.runSilently();
		return docsDeep;
	}
	
	@Nullable
	public final F getSubFolder(String name) {
		if (subFolders == null) return null;
		return subFolders.get(name);
	}
	
	public final int getSubFolderCount() {
		if (subFolders == null) return 0;
		return subFolders.size();
	}
	
	@Immutable
	@NotNull
	public final Collection<F> getSubFolders() {
		return UtilModel.nullSafeImmutableCol(subFolders);
	}
	
	@Immutable
	@NotNull
	public final Map<String, F> getSubFolderMap() {
		return UtilModel.nullSafeImmutableMap(subFolders);
	}
	
	public final int getChildCount() {
		int count = 0;
		if (documents != null)
			count += documents.size();
		if (subFolders != null)
			count += subFolders.size();
		return count;
	}
	
	@Immutable
	@NotNull
	public final Iterable<ViewNode> getChildren() {
		Collection<F> col = subFolders == null ? null : subFolders.values();
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
	
}
