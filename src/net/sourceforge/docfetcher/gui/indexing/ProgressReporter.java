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

package net.sourceforge.docfetcher.gui.indexing;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

/**
 * @author Tran Nam Quang
 */
final class ProgressReporter extends IndexingReporter {
	
	/*
	 * TODO maybe print out some filesize-based info at the end:
	 * - file throughput (MB/sec, files/sec)
	 * - average filesize
	 * take care when calculating filesizes for HTML pairs!
	 */
	
	private final ProgressPanel progressPanel;
	private long lastStart = 0;

	public ProgressReporter(ProgressPanel progressPanel) {
		this.progressPanel = progressPanel;
	}
	
	public void indexingStarted() {
		lastStart = System.currentTimeMillis();
	}
	
	public void indexingStopped() {
		long duration = (System.currentTimeMillis() - lastStart) / 1000;
		String msg = "Duration: " + duration + " s";
		progressPanel.append(msg);
		Util.println(msg);
	}
	
	public void info(	InfoType infoType,
						TreeNode treeNode) {
		progressPanel.append(treeNode.getDisplayName());
	}
	
	public void fail(	ErrorType errorType,
						TreeNode treeNode,
						Throwable cause) {
		progressPanel.append("ERROR " + errorType.name() + ": " + treeNode.getDisplayName());
	}

}
