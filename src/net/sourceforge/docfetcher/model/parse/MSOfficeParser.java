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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hdgf.extractor.VisioTextExtractor;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hslf.exceptions.EncryptedPowerPointFileException;
import org.apache.poi.hslf.exceptions.OldPowerPointFormatException;
import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.poi.hwpf.extractor.Word6Extractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;

import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
abstract class MSOfficeParser extends FileParser {
	
	public static final class MSWordParser extends MSOfficeParser {
		public MSWordParser() {
			// TODO post-release-1.1: 'dot' extension might interfere with GraphViz dot format
			super(Msg.filetype_doc.get(), "doc", "dot");
		}
		protected String extractText(InputStream in)
				throws IOException, ParseException {
			try {
				return new WordExtractor(in).getText();
			}
			catch (OldWordFileFormatException e) {
				return new Word6Extractor(in).getText();
			}
			catch (EncryptedDocumentException e) {
				// Discard throwable cause, we don't want to show its message
				throw new ParseException(Msg.doc_pw_protected.get());
			}
		}
	}
	
	public static final class MSPowerPointParser extends MSOfficeParser {
		public MSPowerPointParser() {
			super(Msg.filetype_ppt.get(), "ppt", "pps");
		}
		protected String extractText(InputStream in)
				throws IOException, ParseException {
			try {
				return new PowerPointExtractor(in).getText(true, true, true, true);
			}
			catch (OldPowerPointFormatException e) {
				throw new ParseException(e);
			}
			catch (EncryptedPowerPointFileException e) {
				// Discard throwable cause, we don't want to show its message
				throw new ParseException(Msg.doc_pw_protected.get());
			}
		}
	}
	
	public static final class MSVisioParser extends MSOfficeParser {
		public MSVisioParser() {
			super(Msg.filetype_vsd.get(), "vsd");
		}
		protected String extractText(InputStream in)
				throws IOException {
			return new VisioTextExtractor(in).getText();
		}
	}
	
	/*
	 * The mime magic detector seems to identify all MS Office files as
	 * 'application/msword', even when they're not MS Word files.
	 */
	private static final Collection<String> types = MediaType.Col.application("msword");
	
	private final String typeLabel;
	private final Collection<String> extensions;
	
	MSOfficeParser(	@NotNull String typeLabel,
					@NotNull String... extensions) {
		this.typeLabel = typeLabel;
		this.extensions = Arrays.asList(extensions);
	}

	protected final ParseResult parse(File file, ParseContext context)
			throws ParseException {
		String contents = renderText(file, context.getFilename());
		ParseResult parseResult = new ParseResult(contents);
		
		POIFSReader reader = new POIFSReader();
		MyReaderListener listener = new MyReaderListener();
		reader.registerListener(listener, "\005SummaryInformation"); //$NON-NLS-1$
		
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			reader.read(in);
			
			parseResult
			.setTitle(listener.title)
			.addAuthor(listener.author)
			.addMiscMetadata(listener.subject)
			.addMiscMetadata(listener.keywords)
			.addMiscMetadata(listener.comments);
		}
		catch (IOException e) {
			// Ignore, we can live without meta data
			Util.printErr(e);
		}
		finally {
			Closeables.closeQuietly(in);
		}
		
		return parseResult;
	}
	
	protected String renderText(File file, String filename)
			throws ParseException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			return extractText(in);
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
		catch (RuntimeException e) {
			/*
			 * As seen in numerous bug reports (3439858, 3439576, 3439057,
			 * 3438353, 3437768, 3437667), Apache POI seems to throw a lot of
			 * runtime exceptions, taking down the entire program with it. To
			 * avoid crashing, we'll intercept all runtime exceptions and turn
			 * them into regular indexing errors.
			 */
			throw new ParseException(e);
		}
		finally {
			Closeables.closeQuietly(in);
		}
	}
	
	@NotNull
	protected abstract String extractText(@NotNull InputStream in)
			throws IOException, ParseException;

	protected final Collection<String> getExtensions() {
		return extensions;
	}

	protected final Collection<String> getTypes() {
		return types;
	}

	public final String getTypeLabel() {
		return typeLabel;
	}
	
	private static final class MyReaderListener implements POIFSReaderListener {
		public String author;
		public String title;
		public String subject;
		public String keywords;
		public String comments;
		
		public void processPOIFSReaderEvent(POIFSReaderEvent event) {
			try {
				SummaryInformation si = (SummaryInformation) PropertySetFactory.create(event.getStream());
				
				// Combine 'author' and 'last author' field if they're identical
				String author;
				String defaultAuthor = si.getAuthor();
				String lastAuthor = si.getLastAuthor();
				if (defaultAuthor.equals(lastAuthor))
					author = defaultAuthor;
				else
					author = defaultAuthor + ", " + lastAuthor; //$NON-NLS-1$
				
				this.author = author;
				title = si.getTitle();
				subject = si.getSubject();
				keywords = si.getKeywords();
				comments = si.getComments();
			}
			catch (Exception e) {
				// Ignore, we can live without meta data
				Util.printErr(e);
			}
		}
	}

}
