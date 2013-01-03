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
 * An implementation of <tt>Parser</tt> that works on <tt>File</tt>s.
 * 
 * @author Tran Nam Quang
 */
abstract class FileParser extends Parser {

	/**
	 * This method extracts text and metadata from the given file for internal
	 * consumption by the indexing engine.
	 * <p>
	 * The given <tt>ParseContext</tt> object provides implementors with
	 * additional information, such as whether the indexing has been cancelled
	 * by the user. In the latter case, this method should stop all parsing as
	 * soon as possible and return any partially extracted text.
	 */
	@NotNull
	protected abstract ParseResult parse(	@NotNull File file,
	                                     	@NotNull ParseContext context)
			throws ParseException;
	
	/**
	 * This method extracts text from the given file for presentation on the
	 * preview pane. The default implementation uses the text extracted from the
	 * <tt>parse</tt> method; implementors may override to output more
	 * human-friendly text.
	 */
	@NotNull
	protected String renderText(@NotNull File file,
								@NotNull String filename)
			throws ParseException {
		ParseContext context = new ParseContext(filename);
		return parse(file, context).getContent().toString();
	}

}
