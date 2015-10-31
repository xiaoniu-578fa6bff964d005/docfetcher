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

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import net.sourceforge.docfetcher.enums.Msg;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.rtf.RTFParser;
import org.apache.tika.sax.BodyContentHandler;

/**
 * @author Tran Nam Quang
 */
final class RtfParser extends StreamParser {
	
	private static final Collection<String> extensions = Collections.singleton("rtf");
	private static final Collection<String> types = MediaType.Col.text("rtf");

	protected ParseResult parse(InputStream in, ParseContext context)
			throws ParseException {
		BodyContentHandler bodyHandler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		try {
			new RTFParser().parse(in, bodyHandler, metadata, ParseService.tikaContext());
			
			// Use this to get a full list of available metadata:
//			for (String name : metadata.names()) {
//				System.out.println(name + " : " + metadata.get(name));
//			}
			
			// RTF documents also have a "comments" field, but Tika's RTF parser
			// doesn't seem to read it.
			return new ParseResult(bodyHandler.toString())
				.addAuthor(metadata.get(TikaCoreProperties.CREATOR))
				.setTitle(metadata.get(TikaCoreProperties.TITLE))
				.addMiscMetadata(metadata.get(Metadata.SUBJECT)) // not equivalent to TikaCoreProperties.KEYWORDS
				.addMiscMetadata(metadata.get(TikaCoreProperties.KEYWORDS));
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
	}
	
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		BodyContentHandler bodyHandler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		try {
			new RTFParser().parse(in, bodyHandler, metadata, ParseService.tikaContext());
			return bodyHandler.toString();
		}
		catch (Exception e) {
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
		return Msg.filetype_rtf.get();
	}

}
