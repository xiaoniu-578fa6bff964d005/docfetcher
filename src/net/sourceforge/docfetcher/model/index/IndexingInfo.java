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

package net.sourceforge.docfetcher.model.index;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.model.TreeNode;

/**
 * @author Tran Nam Quang
 */
public final class IndexingInfo {

	public enum InfoType {
		// Files
		UNPACKING,
		
		// Files and Outlook
		EXTRACTING,
	}
	
	private final InfoType infoType;
	private final TreeNode treeNode;
	
	public IndexingInfo(@NotNull InfoType infoType, @NotNull TreeNode treeNode) {
		Util.checkNotNull(infoType, treeNode);
		this.infoType = infoType;
		this.treeNode = treeNode;
	}

	@NotNull
	public boolean is(@NotNull InfoType infoType) {
		Util.checkNotNull(infoType);
		return this.infoType == infoType;
	}

	@NotNull
	public TreeNode getTreeNode() {
		return treeNode;
	}
	
}
