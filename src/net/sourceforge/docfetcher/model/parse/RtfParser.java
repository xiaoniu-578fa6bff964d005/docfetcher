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

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

/**
 * @author Tran Nam Quang
 */
final class RtfParser extends StreamParser {
	
	// TODO post-release-1.1: Use RTF parser from Tika
	
	private static final Collection<String> extensions = Collections.singleton("rtf");
	private static final Collection<String> types = Collections.singleton(MediaType.text("rtf"));

	protected ParseResult parse(InputStream in, ParseContext context)
			throws ParseException {
		return new ParseResult(renderText(in, context.getFilename()));
	}
	
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		try {
			DefaultStyledDocument doc = new DefaultStyledDocument();
			new RTFEditorKit().read(in, doc, 0);
			return doc.getText(0, doc.getLength());
		}
		catch (BadLocationException e) {
			throw new ParseException(e);
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
	}

	protected Collection<String> getExtensions() {
		return extensions;
	}

	protected Collection<String> getTypes() {
		return types;
	}

	public String getTypeLabel() {
		return "RTF Document"; // TODO i18n filetype_rtf
	}

}
