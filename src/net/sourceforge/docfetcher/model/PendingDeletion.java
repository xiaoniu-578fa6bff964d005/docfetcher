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

package net.sourceforge.docfetcher.model;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.NotThreadSafe;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class PendingDeletion {
	
	private final LuceneIndex index;
	private boolean approvedByQueue;
	private boolean approvedBySearcher;
	
	public PendingDeletion(@NotNull LuceneIndex index) {
		this.index = Util.checkNotNull(index);
	}
	
	@ThreadSafe
	public LuceneIndex getLuceneIndex() {
		return index;
	}

	@ThreadSafe
	public synchronized void setApprovedByQueue() {
		approvedByQueue = true;
		maybeDelete();
	}
	
	@ThreadSafe
	public synchronized void setApprovedBySearcher() {
		approvedBySearcher = true;
		maybeDelete();
	}
	
	@NotThreadSafe
	private void maybeDelete() {
		if (approvedByQueue && approvedBySearcher)
			index.delete();
	}

}
