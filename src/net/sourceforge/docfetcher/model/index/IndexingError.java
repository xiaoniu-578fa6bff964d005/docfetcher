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

import java.io.Serializable;

import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@SuppressWarnings("serial")
@VisibleForPackageGroup
public final class IndexingError implements Serializable {
	
	public enum ErrorType {
		// Files
		ARCHIVE,
		ARCHIVE_UNPACK_DISKSPACE,
		ARCHIVE_ENCRYPTED,
		ARCHIVE_ENTRY,
		ARCHIVE_ENTRY_ENCRYPTED,
		PARSING,
		OUT_OF_MEMORY,
		STACK_OVERFLOW,
		NOT_AN_ARCHIVE,
		ENCODING,
		
		// Outlook
		ATTACHMENT,
		
		// Files and Outlook
		IO_EXCEPTION,
	}
	
	private final ErrorType errorType;
	private final TreeNode treeNode;
	@Nullable private final Throwable throwable;
	
	// Throwable can be null to indicate cause is unavailable / unknown
	public IndexingError(	@NotNull ErrorType errorType,
							@NotNull TreeNode treeNode,
							@Nullable Throwable throwable) {
		Util.checkNotNull(errorType, treeNode);
		this.errorType = errorType;
		this.treeNode = treeNode;
		this.throwable = throwable;
	}

	@NotNull
	public ErrorType getErrorType() {
		return errorType;
	}

	@NotNull
	public TreeNode getTreeNode() {
		return treeNode;
	}

	@Nullable
	public Throwable getThrowable() {
		return throwable;
	}
	
}


