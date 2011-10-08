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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
@SuppressWarnings("serial")
public abstract class TreeNode implements Serializable {
	
	private final String name;
	private final String displayName;
	
	/**
	 * The indexing errors that occurred on this tree node the last time the
	 * index was updated. Null if no error occurred during the last update.
	 */
	@Nullable private List<IndexingError> errors; // Null instead of empty list to save some RAM
	
	public TreeNode(@NotNull String name,
					@NotNull String displayName) {
		Util.checkNotNull(name, displayName);
		this.name = name;
		this.displayName = displayName;
	}
	
	@NotNull
	public final String getName() {
		return name;
	}
	
	@NotNull
	public final String getDisplayName() {
		return displayName;
	}
	
	@NotNull
	public abstract Path getPath();
	
	@NotNull
	public final String toString() {
		return getPath().toString();
	}

	@Immutable
	@NotNull
	public synchronized final List<IndexingError> getErrors() {
		return errors == null
			? Collections.<IndexingError> emptyList()
			: errors;
	}
	
	public synchronized final boolean hasErrors() {
		return errors != null && !errors.isEmpty();
	}
	
	public synchronized final void setError(@Nullable IndexingError error) {
		this.errors = error == null ? null : Collections.singletonList(error);
	}

	public synchronized final void setErrors(@Nullable List<IndexingError> errors) {
		this.errors = errors == null ? null : ImmutableList.copyOf(errors);
	}
	
}