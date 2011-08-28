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

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.model.index.IndexingError;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
@SuppressWarnings("serial")
public abstract class TreeNode implements Serializable {
	
	private final String name;
	private final String displayName;
	
	/**
	 * The indexing error that occurred on this tree node the last time the
	 * index was updated. Null if no error occurred during the last update.
	 */
	@Nullable private IndexingError error;
	
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
	public abstract String getPath();
	
	@NotNull
	public final String toString() {
		return getPath();
	}

	@Nullable
	public final IndexingError getError() {
		return error;
	}

	public final void setError(@Nullable IndexingError error) {
		this.error = error;
	}
	
}