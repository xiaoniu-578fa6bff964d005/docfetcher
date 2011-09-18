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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.collect.BoundedList;

/**
 * @author Tran Nam Quang
 */
public final class DelegatingReporter extends IndexingReporter {

	public interface ExistingMessagesHandler {
		public void handleMessages(	@MutableCopy @NotNull List<IndexingInfo> infos,
									@MutableCopy @NotNull List<IndexingError> errors);
	}

	@Nullable private IndexingReporter delegate;
	private final BoundedList<IndexingInfo> infos;
	private final List<IndexingError> errors;
	@Nullable private Long start;
	@Nullable private Long end;

	DelegatingReporter(int infoCapacity) {
		infos = new BoundedList<IndexingInfo>(infoCapacity);
		errors = new ArrayList<IndexingError>();
	}

	public void attachDelegate(	@NotNull IndexingReporter delegate,
								@NotNull ExistingMessagesHandler handler) {
		Util.checkNotNull(delegate, handler);
		Util.checkThat(this.delegate == null);
		
		List<IndexingInfo> infoSnapshot;
		List<IndexingError> errorSnapshot;
		
		synchronized (this) {
			this.delegate = delegate;
			if (start != null)
				delegate.setStartTime(start);
			if (end != null)
				delegate.setEndTime(end);
			infoSnapshot = new ArrayList<IndexingInfo>(infos);
			errorSnapshot = new ArrayList<IndexingError>(errors);
		}
		
		handler.handleMessages(infoSnapshot, errorSnapshot);
	}

	public synchronized void detachDelegate(@NotNull IndexingReporter delegate) {
		Util.checkNotNull(delegate);
		Util.checkThat(this.delegate == delegate);
		this.delegate = null;
	}

	public synchronized void setStartTime(long time) {
		start = time;
		if (delegate != null)
			delegate.setStartTime(time);
	}

	public synchronized void setEndTime(long time) {
		end = time;
		if (delegate != null)
			delegate.setEndTime(time);
	}

	public synchronized void info(@NotNull IndexingInfo info) {
		infos.add(info);
		if (delegate != null)
			delegate.info(info);
	}
	
	public synchronized void subInfo(int current, int total) {
		Util.checkThat(!infos.isEmpty());
		infos.getLast().setPercentage(current, total);
		if (delegate != null)
			delegate.subInfo(current, total);
	}

	public synchronized void fail(@NotNull IndexingError error) {
		errors.add(error);
		if (delegate != null)
			delegate.fail(error);
	}

}