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

import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.model.index.IndexingInfo;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
final class ProgressReporter extends IndexingReporter {
	
	/*
	 * TODO post-release-1.1: maybe print out some filesize-based info at the end:
	 * - file throughput (MB/sec, files/sec)
	 * - average filesize
	 * take care when calculating filesizes for HTML pairs!
	 */
	
	// TODO i18n
	
	private final ProgressTable progressTable;
	private final ErrorTable errorTable;
	private long start = 0;
	@Nullable private IndexingInfo lastInfo;

	public ProgressReporter(@NotNull ProgressPanel progressPanel) {
		progressTable = progressPanel.getProgressTable();
		errorTable = progressPanel.getErrorTable();
	}
	
	public void setStartTime(long time) {
		start = time;
	}
	
	public void setEndTime(long time) {
		long duration = (time - start) / 1000;
		String msg = "Duration: " + duration + " s";
		progressTable.append(msg);
		Util.println(msg);
	}
	
	public void info(@NotNull IndexingInfo info) {
		progressTable.append(getMessage(info));
		lastInfo = info;
	}
	
	public void subInfo(int current, int total) {
		Util.checkThat(lastInfo != null);
		String message = getMessage(lastInfo);
		message = String.format("%s [%d/%d]", message, current, total);
		progressTable.replaceLast(message);
	}
	
	@NotNull
	private String getMessage(@NotNull IndexingInfo info) {
		String displayName = info.getTreeNode().getDisplayName();
		return String.format("%,d\t %s", info.getNumber(), displayName);
	}
	
	public void fail(@NotNull IndexingError error) {
		progressTable.append("ERROR " + error.getErrorType().name() + ": "
				+ error.getTreeNode().getDisplayName());
		errorTable.addError(error);
	}

}
