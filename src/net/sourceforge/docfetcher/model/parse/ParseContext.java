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

package net.sourceforge.docfetcher.model.parse;

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
final class ParseContext {
	
	private final String filename;
	private final IndexingReporter reporter;
	private final Cancelable cancelable;

	public ParseContext(@NotNull String filename) {
		this(filename, IndexingReporter.nullReporter, Cancelable.nullCancelable);
	}
	
	public ParseContext(@NotNull String filename,
	                    @NotNull IndexingReporter reporter,
						@NotNull Cancelable cancelable) {
		Util.checkNotNull(filename, reporter, cancelable);
		this.filename = filename;
		this.reporter = reporter;
		this.cancelable = cancelable;
	}
	
	@NotNull
	public String getFilename() {
		return filename;
	}

	@NotNull
	public IndexingReporter getReporter() {
		return reporter;
	}

	@NotNull
	public Cancelable getCancelable() {
		return cancelable;
	}

}
