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

import java.io.File;

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

/**
 * @author Tran Nam Quang
 */
abstract class FileParser extends Parser {

	// If the indexing is canceled, this method should stop all parsing
	// immediately if possible and return the partially extracted text.
	@NotNull
	protected abstract ParseResult parse(	@NotNull File file,
	                                     	@NotNull IndexingReporter reporter,
											@NotNull Cancelable cancelable)
			throws ParseException;
	
	@NotNull
	protected String renderText(@NotNull File file,
								@NotNull Cancelable cancelable)
			throws ParseException {
		ParseResult parseResult = parse(file, IndexingReporter.nullReporter, cancelable);
		return parseResult.getContent().toString();
	}

}
