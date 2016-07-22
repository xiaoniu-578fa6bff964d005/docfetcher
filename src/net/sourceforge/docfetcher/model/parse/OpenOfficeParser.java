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
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.ZipException;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.odf.OpenDocumentParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 * @author Tran Nam Quang
 */
abstract class OpenOfficeParser extends StreamParser {
	
	public static final class OpenOfficeWriterParser extends OpenOfficeParser {
		public OpenOfficeWriterParser() {
			super(Msg.filetype_odt.get(), "odt", "ott");
		}
	}
	
	public static final class OpenOfficeCalcParser extends OpenOfficeParser {
		public OpenOfficeCalcParser() {
			super(Msg.filetype_ods.get(), "ods", "ots");
		}
	}
	
	public static final class OpenOfficeDrawParser extends OpenOfficeParser {
		public OpenOfficeDrawParser() {
			super(Msg.filetype_odg.get(), "odg", "otg");
		}
	}
	
	public static final class OpenOfficeImpressParser extends OpenOfficeParser {
		public OpenOfficeImpressParser() {
			super(Msg.filetype_odp.get(), "odp", "otp");
		}
	}
	
	private static final Collection<String> types = MediaType.Col.application("zip");
	
	private final String typeLabel;
	private final Collection<String> extensions;
	
	private OpenOfficeParser(@NotNull String typeLabel, @NotNull String... extensions) {
		this.typeLabel = typeLabel;
		this.extensions = Arrays.asList(extensions);
	}
	
	protected final Collection<String> getExtensions() {
		return extensions;
	}
	
	public final String getTypeLabel() {
		return typeLabel;
	}
	
	protected final Collection<String> getTypes() {
		return types;
	}
	
	@Override
	protected final ParseResult parse(	@NotNull InputStream in,
										@NotNull ParseContext context)
			throws ParseException {
		OpenDocumentParser tikaParser = new OpenDocumentParser();
		BodyContentHandler contentHandler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		try {
			tikaParser.parse(in, contentHandler, metadata, ParseService.tikaContext());
			ParseResult result = new ParseResult(contentHandler.toString())
				.setTitle(metadata.get(TikaCoreProperties.TITLE))
				.addAuthor(metadata.get(TikaCoreProperties.CREATOR))
				.addMiscMetadata(metadata.get(TikaCoreProperties.DESCRIPTION))
				.addMiscMetadata(metadata.get(TikaCoreProperties.TRANSITION_SUBJECT_TO_OO_SUBJECT))
				.addMiscMetadata(metadata.get(TikaCoreProperties.KEYWORDS));
			for (String name : metadata.names()) {
				if (name.startsWith("custom:")) {
					result.addMiscMetadata(metadata.get(name));
				}
			}
			return result;
		}
		catch (ZipException e) {
			String msg = "only DEFLATED entries can have EXT descriptor";
			if (e.getMessage().equals(msg)) {
				throw new ParseException(Msg.doc_pw_protected.get());
			}
			else {
				throw new ParseException(e);
			}
		}
		catch (IOException | SAXException | TikaException e) {
			throw new ParseException(e);
		}
	}

}
