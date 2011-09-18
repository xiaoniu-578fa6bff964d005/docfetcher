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

import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

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
	private final int number;
	
	@Nullable private int[] percentage;
	
	public IndexingInfo(@NotNull InfoType infoType, @NotNull TreeNode treeNode, int number) {
		Util.checkNotNull(infoType, treeNode);
		this.infoType = infoType;
		this.treeNode = treeNode;
		this.number = number;
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
	
	public int getNumber() {
		return number;
	}

	@Nullable
	public int[] getPercentage() {
		return percentage;
	}

	public void setPercentage(@Nullable int... percentage) {
		Util.checkThat(percentage == null || percentage.length == 2);
		this.percentage = percentage;
	}
	
}
