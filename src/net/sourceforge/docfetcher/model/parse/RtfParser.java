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
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import net.sourceforge.docfetcher.enums.Msg;

import org.apache.tika.config.TikaConfig;
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
			/*
			 * Bug #1230: If the RTF file contains an image, the RTF parser will
			 * look for Tika parsers that can parse the image. This involves
			 * reading the tika-mimetypes.xml file, which cannot be found
			 * without the following custom class loader, due to the fact that
			 * we're running Tika outside its expected environment.
			 */
			org.apache.tika.parser.ParseContext tikaContext = ParseService.tikaContext();
			TikaConfig tikaConfig = new TikaConfig(new ClassLoader() {
				protected URL findResource(String name) {
					if ("tika-mimetypes.xml".equals(name)) {
						return getResource("org/apache/tika/tika-mimetypes.xml");
					}
			        return null;
			    }
			});
			tikaContext.set(TikaConfig.class, tikaConfig);
			
			new RTFParser().parse(in, bodyHandler, metadata, tikaContext);
			
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
		try {
			new RTFParser().parse(in, bodyHandler, metadata, ParseService.tikaContext());
			return bodyHandler.toString();
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
