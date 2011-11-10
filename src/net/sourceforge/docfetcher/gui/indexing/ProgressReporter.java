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

import net.sourceforge.docfetcher.enums.Msg;
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
		String duration = toHumanReadableDuration(time - start);
		String msg = Msg.total_elapsed_time.format(duration);
		progressTable.append(msg);
	}
	
	/**
	 * Converts the given period of time in milliseconds into something more
	 * human-friendly (e.g. "1 h 24 min 3 s").
	 */
	@NotNull
	private static String toHumanReadableDuration(long millis) {
		int secs = (int) (millis / 1000);
		int hrs = secs / 3600;
		secs -= hrs * 3600;
		int mins = secs / 60;
		secs -= mins * 60;
		String ret = ""; //$NON-NLS-1$
		if (hrs != 0)
			ret += hrs + " h"; //$NON-NLS-1$
		if (mins != 0)
			ret += (hrs == 0 ? "" : " ") + mins + " min"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (secs != 0)
			ret += (hrs == 0 && mins == 0 ? "" : " ") + secs + " s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (ret.equals("")) //$NON-NLS-1$
			return "0 s"; //$NON-NLS-1$
		return ret;
	}
	
	public void info(@NotNull IndexingInfo info) {
		String message = getMessage(info);
		int[] percentage = info.getPercentage();
		if (percentage != null)
			message = String.format("%s [%d/%d]", message, percentage[0], percentage[1]);
		progressTable.append(message);
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
		String displayName = error.getTreeNode().getDisplayName();
		progressTable.append("### " + Msg.error.format(displayName));
		errorTable.addError(error);
	}

}
