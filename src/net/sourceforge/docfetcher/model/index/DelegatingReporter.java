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

import net.sourceforge.docfetcher.base.BoundedList;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
public final class DelegatingReporter extends IndexingReporter {

	public interface ExistingMessagesHandler {
		// The given lists are mutable
		public void handleMessages(	@NotNull List<IndexingInfo> infos,
									@NotNull List<IndexingError> errors);
	}

	public interface ExistingMessagesProvider {
		@NotNull
		public List<IndexingInfo> getInfos();

		@NotNull
		public List<IndexingError> getErrors();
	}

	@Nullable private IndexingReporter delegate;
	private final BoundedList<IndexingInfo> infos;
	private final List<IndexingError> errors;

	DelegatingReporter(int infoCapacity) {
		infos = new BoundedList<IndexingInfo>(infoCapacity);
		errors = new ArrayList<IndexingError>();
	}

	public synchronized void attachDelegate(@NotNull IndexingReporter delegate,
											@NotNull ExistingMessagesHandler handler) {
		Util.checkNotNull(delegate, handler);
		Util.checkThat(this.delegate == null);
		this.delegate = delegate;
		List<IndexingError> errorsCopy = new ArrayList<IndexingError>(errors);
		handler.handleMessages(infos.removeAll(), errorsCopy);
		errors.clear();
	}

	public synchronized void detachDelegate(@NotNull IndexingReporter delegate,
											@NotNull ExistingMessagesProvider provider) {
		Util.checkNotNull(delegate, provider);
		Util.checkThat(this.delegate == delegate);
		Util.checkThat(infos.isEmpty() && errors.isEmpty());
		this.delegate = null;
		infos.addAll(provider.getInfos());
		errors.addAll(provider.getErrors());
	}

	public synchronized void indexingStarted() {
		if (delegate != null)
			delegate.indexingStarted();
	}

	public synchronized void indexingStopped() {
		if (delegate != null)
			delegate.indexingStopped();
	}

	public synchronized void info(@NotNull IndexingInfo info) {
		if (delegate != null)
			delegate.info(info);
		else
			infos.add(info);
	}
	
	public synchronized void subInfo(int current, int total) {
		if (delegate != null) {
			delegate.subInfo(current, total);
		}
		else {
			Util.checkThat(!infos.isEmpty());
			infos.getLast().setPercentage(current, total);
		}
	}

	public synchronized void fail(@NotNull IndexingError error) {
		if (delegate != null)
			delegate.fail(error);
		else
			errors.add(error);
	}

}