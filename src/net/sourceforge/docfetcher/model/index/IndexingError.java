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

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class IndexingError implements Serializable {
	
	public enum ErrorType {
		// Files
		ARCHIVE,
		ARCHIVE_UNPACK_DISKSPACE,
		ARCHIVE_ENCRYPTED (Msg.archive_encrypted.get()),
		ARCHIVE_ENTRY,
		ARCHIVE_ENTRY_ENCRYPTED (Msg.archive_entry_encrypted.get()),
		PARSING,
		OUT_OF_MEMORY (Msg.out_of_memory_instructions_brief.get()),
		NOT_AN_ARCHIVE (Msg.not_an_archive.get()),
		ENCODING,
		STACK_OVERFLOW,
		
		// Outlook
		ATTACHMENT,
		
		// Files and Outlook
		IO_EXCEPTION,
		;
		
		@Nullable private String overrideMsg;
		
		private ErrorType() {
		}
		private ErrorType(@Nullable String overrideMsg) {
			this.overrideMsg = overrideMsg;
		}
		
		@NotNull
		private String getMessage(@Nullable Throwable t) {
			if (overrideMsg != null)
				return overrideMsg;
			return Util.getLowestMessage(t);
		}
	}
	
	private static final long serialVersionUID = 1L;
	
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
	
	@NotNull
	public String getLocalizedMessage() {
		return errorType.getMessage(throwable);
	}
	
}


