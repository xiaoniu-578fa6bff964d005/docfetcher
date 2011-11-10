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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import net.sourceforge.docfetcher.enums.Msg;

/**
 * @author Tran Nam Quang
 */
final class SvgParser extends StreamParser {
	
	private static final Collection<String> extensions = Collections.singleton("svg");
	private static final Collection<String> types = MediaType.Col.text("xml");

	@Override
	protected ParseResult parse(InputStream in, ParseContext context)
			throws ParseException {
		try {
			Source source = new Source(in);
			source.setLogger(null);
			return new ParseResult(source.getTextExtractor().toString())
				.setTitle(getElementContent(source, "dc:title"))
				.addAuthor(getElementContent(source, "dc:creator"));
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
	}
	
	@Override
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		try {
			Source source = new Source(in);
			source.setLogger(null);
			return source.getTextExtractor().toString();
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
	}
	
	/**
	 * Returns the textual content inside the given HTML element from the given
	 * HTML source. Returns null if the HTML element is not found.
	 */
	private String getElementContent(Source source, String elementName) {
		Element el = source.getNextElement(0, elementName);
		return el == null ? null : CharacterReference.decode(el.getTextExtractor().toString());
	}

	protected Collection<String> getExtensions() {
		return extensions;
	}

	protected Collection<String> getTypes() {
		return types;
	}

	public String getTypeLabel() {
		return Msg.filetype_svg.get();
	}

}
