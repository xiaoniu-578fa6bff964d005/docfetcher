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

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public abstract class Document<D extends Document<D, F>, F extends Folder<D, F>> extends TreeNode {
	
	private static final long serialVersionUID = 1L;
	
	@NotNull F parent;
	private long lastModified = -1;
	
	// will replace document with identical name in parent
	@SuppressWarnings("unchecked")
	public Document(@NotNull F parent,
	                @NotNull String name,
	                @NotNull String displayName,
					long lastModified) {
		super(name, displayName);
		parent.putDocument((D) this); // Will set parent field for this instance
		this.lastModified = lastModified;
	}

	public final long getLastModified() {
		return lastModified;
	}
	
	public final void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	@NotNull
	protected abstract DocumentType getType();
	
	@NotNull
	public final String getUniqueId() {
		return getType().createUniqueId(getPath());
	}
	
	@NotNull
	public final String getPath() {
		return Util.joinPath(parent.getPath(), getName());
	}

}
