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
import org.apache.tika.parser.rtf.RTFEmbObjHandler;
import org.apache.tika.parser.rtf.TextExtractor;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;

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
		TextExtractor extractor = createExtractor(bodyHandler, metadata);
		try {
			extractor.extract(in);
			
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
		catch (AssertionError e) {
			/*
			 * With the RTF parser in Tika 0.10, calling TextExtractor.extract
			 * results in an AssertionError. See bug #3443948.
			 */
			throw new ParseException(e);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
	}
	
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		BodyContentHandler bodyHandler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		TextExtractor extractor = createExtractor(bodyHandler, metadata);
		try {
			extractor.extract(in);
			return bodyHandler.toString();
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
	}
	
	private static TextExtractor createExtractor(BodyContentHandler bodyHandler, Metadata metadata) {
		XHTMLContentHandler handler = new XHTMLContentHandler(bodyHandler, metadata);
		org.apache.tika.parser.ParseContext context = new org.apache.tika.parser.ParseContext();
		RTFEmbObjHandler embObjHandler = new RTFEmbObjHandler(handler, metadata, context);
		return new TextExtractor(handler, metadata, embObjHandler);
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
