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

import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
abstract class FileParser extends Parser {

	// If the indexing is canceled, this method should stop all parsing
	// immediately if possible and return the partially extracted text.
	@NotNull
	protected abstract ParseResult parse(	@NotNull File file,
	                                     	@NotNull ParseContext context)
			throws ParseException;
	
	@NotNull
	protected String renderText(@NotNull File file,
								@NotNull String filename)
			throws ParseException {
		ParseContext context = new ParseContext(filename);
		return parse(file, context).getContent().toString();
	}

}
