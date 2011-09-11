/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.parse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.NullCancelable;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.google.common.io.Closeables;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;


/**
 * @author Tran Nam Quang
 */
public final class ParseService {

	/*
	 * TODO base: Extraction registry must handle archive entries as inputs
	 * transparently, including disk space check when extracting the file ->
	 * document this
	 * 
	 * TODO Implement support for filename-only search: Requires a
	 * FilenameParser which is used in case of failure of another parser, or
	 * when the filetype is unkown.
	 * 
	 * TODO Some parsers also accept InputStream rather than File (e.g. HTML
	 * parser and Text parser), so unpacking from a PST or an archive may not
	 * always be necessary.
	 */
	
	/*
	 * Construction of this object seems relatively expensive, so we'll keep a
	 * single instance of it.
	 */
	private static final MagicMimeMimeDetector mimeDetector = new MagicMimeMimeDetector();
	
	private static final TextParser textParser;
	private static final HtmlParser htmlParser;
	
	private static final Parser[] parsers = {
		// TextParser must have high priority since its file extensions can be customized
		textParser = new TextParser(),
		htmlParser = new HtmlParser(),
		new PdfParser()
	};

	private ParseService() {}
	
	@Immutable
	@NotNull
	public static List<Parser> getParsers() {
		return Arrays.asList(parsers);
	}
	
	// Accepts HTML files, but does not handle HTML pairing
	// may throw OutOfMemoryErrors
	// When parsing a temporary file and mime type detection is off, the temporary file
	// must have the correct file extension.
	@NotNull
	public static ParseResult parse(@NotNull IndexingConfig config,
	                                @NotNull String filename,
	                                @NotNull File file,
	                                @NotNull IndexingReporter reporter,
	                                @NotNull Cancelable cancelable)
			throws ParseException {
		if (config.matchesMimePattern(filename)) {
			try {
				List<String> mimeTypes = getPossibleMimeTypes(file);
				for (Parser parser : parsers) {
					try {
						if (! Collections.disjoint(mimeTypes, parser.getTypes()))
							return doParse(config, parser, file, reporter, cancelable);
					} catch (ParseException e) {
						// Try next parser
					}
				}
			}
			catch (IOException e) {
				// Ignore and continue with detecting the type by filename
			}
		}
		Parser parser = findParser(config, file.getName());
		if (parser == null)
			throw new ParseException(new ParserNotFoundException());
		return doParse(config, parser, file, reporter, cancelable);
	}

	// accepts TrueZIP files
	@NotNull
	private static ParseResult doParse(	@NotNull IndexingConfig config,
										@NotNull Parser parser,
										@NotNull File file,
										@NotNull IndexingReporter reporter,
										@NotNull Cancelable cancelable) throws ParseException {
		ParseResult result = null;
		if (parser instanceof StreamParser) {
			InputStream in = null;
			try {
				if (isZipEntry(file))
					in = new TFileInputStream(file);
				else
					in = new FileInputStream(file);
				StreamParser streamParser = (StreamParser) parser;
				result = streamParser.parse(in, reporter, cancelable);
			}
			catch (FileNotFoundException e) {
				throw new ParseException(e);
			}
			finally {
				Closeables.closeQuietly(in);
			}
		}
		else if (parser instanceof OOoParser) {
			// The OOo parser can handle both regular files and TrueZIP files
			OOoParser oooParser = (OOoParser) parser;
			result = oooParser.parse(file, reporter, cancelable);
		}
		else if (parser instanceof FileParser) {
			FileParser fileParser = (FileParser) parser;
			if (isZipEntry(file)) {
				// Unpack zip entry to temporary file
				TFile tzFile = (TFile) file;
				File tempFile = null;
				try {
					tempFile = config.createDerivedTempFile(tzFile.getName());
					tzFile.cp(tempFile);
					result = fileParser.parse(tempFile, reporter, cancelable);
				}
				catch (IndexingException e) {
					throw new ParseException(e.getIOException());
				}
				catch (IOException e) {
					throw new ParseException(e);
				}
				finally {
					if (tempFile != null)
						tempFile.delete();
				}
			} else {
				result = fileParser.parse(file, reporter, cancelable);
			}
		} else {
			throw new IllegalStateException();
		}
		String parserName = parser.getClass().getSimpleName();
		return result.setParserName(parserName);
	}
	
	private static boolean isZipEntry(@NotNull File file) {
		return file instanceof TFile && ((TFile) file).isEntry();
	}
	
	// does not accept TrueZIP files
	// can only handle files, not emails or other types
	// may throw OutOfMemoryErrors
	@NotNull
	public static String renderText(@NotNull IndexingConfig config,
	                                @NotNull File file,
									@NotNull String parserName)
			throws ParseException {
		Util.checkThat(! (file instanceof TFile));
		NullCancelable cancelable = NullCancelable.getInstance();
		for (Parser parser : parsers) {
			if (!parser.getClass().getSimpleName().equals(parserName))
				continue;
			if (parser instanceof StreamParser) {
				InputStream in = null;
				try {
					in = new FileInputStream(file);
					return ((StreamParser) parser).renderText(in, cancelable);
				}
				catch (FileNotFoundException e) {
					throw new ParseException(e);
				}
				finally {
					Closeables.closeQuietly(in);
				}
			}
			else if (parser instanceof FileParser) {
				return ((FileParser) parser).renderText(file, cancelable);
			}
		}
		throw new IllegalArgumentException(); // TODO make this a checked exception?
	}
	
	@Nullable
	private static Parser findParser(	@NotNull IndexingConfig config,
										@NotNull String filename) {
		String ext = Util.getExtension(filename);
		for (Parser parser : parsers) {
			Collection<String> exts;
			if (parser == textParser)
				exts = config.getTextExtensions();
			else if (parser == htmlParser)
				exts = config.getHtmlExtensions();
			else
				exts = parser.getExtensions();
			for (String candidateExt : exts)
				if (candidateExt.toLowerCase().equals(ext))
					return parser;
		}
		return null;
	}
	
	public static boolean canParseByName(	@NotNull IndexingConfig config,
											@NotNull String filename) {
		return findParser(config, filename) != null;
	}
	
	// accepts TrueZIP files, returns false on IOExceptions
	@MutableCopy
	@NotNull
	private static List<String> getPossibleMimeTypes(@NotNull File file)
			throws IOException {
		InputStream in = null;
		try {
			in = new TFileInputStream(file);
			in = new BufferedInputStream(in); // must support mark and reset
			Collection<?> mimeTypes = mimeDetector.getMimeTypes(in);
			List<String> result = new ArrayList<String> (mimeTypes.size());
			for (Object mimeType : mimeTypes)
				result.add(mimeType.toString().toLowerCase(Locale.ENGLISH));
			return result;
		}
		finally {
			Closeables.closeQuietly(in);
		}
	}

}
