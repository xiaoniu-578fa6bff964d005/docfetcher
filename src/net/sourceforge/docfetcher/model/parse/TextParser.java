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

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.CharsetDetectorHelper;

/**
 * @author Tran Nam Quang
 */
public final class TextParser extends StreamParser {
	
	private final Collection<String> types = MediaType.Col.text("plain");
	
	TextParser() {
	}

	@Override
	protected ParseResult parse(InputStream in,
	                            ParseContext context) throws ParseException {
		try {
			String contents = CharsetDetectorHelper.toString(in);
			return new ParseResult(contents);
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
	}
	
	protected Collection<String> getExtensions() {
		throw new UnsupportedOperationException();
	}
	
	protected Collection<String> getTypes() {
		return types;
	}
	
	public String getTypeLabel() {
		return Msg.filetype_txt.get();
	}

}
