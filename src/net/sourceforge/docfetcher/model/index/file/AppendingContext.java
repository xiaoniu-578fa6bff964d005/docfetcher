/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.index.file;

import java.io.IOException;

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingReporter.ErrorType;

import org.apache.lucene.document.Document;

/**
 * @author Tran Nam Quang
 */
final class AppendingContext extends FileContext {
	
	private final FileContext outerContext;

	public AppendingContext(@NotNull FileContext context) {
		super(
				context.getConfig(),
				context.getZipDetector(),
				new AppendingDocWriter(),
				context.getReporter(),
				context.getOriginalPath(),
				context.getStopper()
		);
		this.outerContext = context;
	}
	
	// returns success
	public boolean appendToOuter(	@NotNull FileDocument doc,
									boolean isAdded) throws IndexingException {
		AppendingDocWriter writer = (AppendingDocWriter) getWriter();
		Document luceneDoc = writer.getLuceneDoc();
		try {
			outerContext.getWriter().write(doc, luceneDoc, isAdded);
			return true;
		} catch (IOException e) {
			throw new IndexingException(e);
		} catch (OutOfMemoryError e) {
			outerContext.getReporter().fail(ErrorType.OUT_OF_MEMORY, doc, e);
		}
		return false;
	}

}