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

import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.model.TreeNode;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public class IndexingReporter {
	
	public enum InfoType {
		// Files
		UNPACKING,
		
		// Files and Outlook
		EXTRACTING,
	}
	
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
		
		// Outlook
		ATTACHMENT,
		
		// Files and Outlook
		IO_EXCEPTION,
	}
	
	public void indexingStarted() {}
	
	public void indexingStopped() {}
	
	public void info(InfoType infoType, TreeNode treeNode) {}
	
	// Throwable can be null to indicate cause is unavailable / unknown
	public void fail(ErrorType errorType, TreeNode treeNode, @Nullable Throwable cause) {}
	
}
