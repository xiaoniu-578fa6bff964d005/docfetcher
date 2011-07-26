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
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public abstract class TreeNode implements Serializable {
	
	private static final long serialVersionUID = -7806671503719389615L;
	
	@NotNull private String name;
	@NotNull private String displayName;
	
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
	
	protected final void setName(@NotNull String name) {
		this.name = Util.checkNotNull(name);
	}
	
	@NotNull
	public final String getDisplayName() {
		return displayName;
	}

	public final void setDisplayName(@NotNull String displayName) {
		this.displayName = Util.checkNotNull(displayName);
	}
	
	@NotNull
	public abstract String getPath();
	
	@NotNull
	public final String toString() {
		return getPath();
	}
	
}