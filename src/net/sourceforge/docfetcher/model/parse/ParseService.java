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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.model.index.PatternAction.MatchAction;
import net.sourceforge.docfetcher.model.parse.OOoParser.OOoCalcParser;
import net.sourceforge.docfetcher.model.parse.OOoParser.OOoDrawParser;
import net.sourceforge.docfetcher.model.parse.OOoParser.OOoImpressParser;
import net.sourceforge.docfetcher.model.parse.OOoParser.OOoWriterParser;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;


/**
 * @author Tran Nam Quang
 */
public final class ParseService {

	/*
	 * Construction of this object seems relatively expensive, so we'll keep a
	 * single instance of it.
	 */
	private static final MagicMimeMimeDetector mimeDetector = new MagicMimeMimeDetector();
	private static final String FILENAME_PARSER = "FilenameParser";
	
	private static final TextParser textParser;
	private static final HtmlParser htmlParser;
	
	private static final Parser[] parsers = {
		// TextParser must have high priority since its file extensions can be customized
		textParser = new TextParser(),
		htmlParser = new HtmlParser(),
		new PdfParser(),
		new AbiWordParser(),
		new OOoWriterParser(),
		new OOoCalcParser(),
		new OOoDrawParser(),
		new OOoImpressParser()
	};

	private ParseService() {}
	
	@Immutable
	@NotNull
	public static List<Parser> getParsers() {
		return Arrays.asList(parsers);
	}
	
	// Accepts HTML files, but does not handle HTML pairing
	// When parsing a temporary file and mime type detection is off, the temporary file
	// must have the correct file extension.
	// Accepts TrueZIP files
	@NotNull
	public static ParseResult parse(@NotNull IndexingConfig config,
	                                @NotNull File file,
	                                @NotNull ParseContext context)
			throws ParseException, CheckedOutOfMemoryError {
		// Search for appropriate parser by mimetype
		for (PatternAction patternAction : config.getPatternActions()) {
			if (patternAction.getAction() != MatchAction.DETECT_MIME)
				continue;
			String filename = context.getFilename();
			Path filepath = context.getFilepath();
			if (!patternAction.matches(filename, filepath, true))
				continue;
			try {
				List<String> mimeTypes = getPossibleMimeTypes(file);
				for (Parser parser : parsers) {
					if (Collections.disjoint(mimeTypes, parser.getTypes()))
						continue;
					try {
						return doParse(config, parser, file, context);
					}
					catch (ParseException e) {
						// Try next parser
					}
				}
			}
			catch (IOException e) {
				// Ignore and continue with detecting the type by filename
			}
		}
		
		// Search for appropriate parser by filename
		Parser parser = findParser(config, file.getName());
		if (parser != null)
			return doParse(config, parser, file, context);
		
		/*
		 * Fall back to filename parser if allowed. The filename will be added
		 * to the contents later.
		 */
		if (config.isIndexFilenames())
			return new ParseResult("").setParserName(FILENAME_PARSER);
		
		throw new ParseException(new ParserNotFoundException());
	}

	// accepts TrueZIP files
	@NotNull
	private static ParseResult doParse(	@NotNull IndexingConfig config,
										@NotNull Parser parser,
										@NotNull File file,
										@NotNull ParseContext context)
			throws ParseException, CheckedOutOfMemoryError {
		try {
			ParseResult result = null;
			if (ProgramConf.Bool.DryRun.get()) {
				result = new ParseResult("");
			}
			else if (parser instanceof StreamParser) {
				InputStream in = null;
				try {
					if (isZipEntry(file))
						in = new TFileInputStream(file);
					else
						in = new FileInputStream(file);
					StreamParser streamParser = (StreamParser) parser;
					result = streamParser.parse(in, context);
				}
				catch (FileNotFoundException e) {
					throw new ParseException(e);
				}
				finally {
					Closeables.closeQuietly(in);
				}
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
						result = fileParser.parse(tempFile, context);
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
				}
				else {
					result = fileParser.parse(file, context);
				}
			}
			else {
				throw new IllegalStateException();
			}
			String parserName = parser.getClass().getSimpleName();
			return result.setParserName(parserName);
		}
		catch (OutOfMemoryError e) {
			throw new CheckedOutOfMemoryError(e);
		}
	}
	
	private static boolean isZipEntry(@NotNull File file) {
		return file instanceof TFile && ((TFile) file).isEntry();
	}
	
	// does not accept TrueZIP files
	// may throw OutOfMemoryErrors
	@NotNull
	public static String renderText(@NotNull IndexingConfig config,
	                                @NotNull File file,
									@NotNull String parserName,
									@NotNull ParseContext context)
			throws ParseException, CheckedOutOfMemoryError {
		Util.checkThat(! (file instanceof TFile));
		if (parserName.equals(FILENAME_PARSER)) {
			assert config.isIndexFilenames();
			return "";
		}
		
		for (Parser parser : parsers) {
			if (!parser.getClass().getSimpleName().equals(parserName))
				continue;
			try {
				if (parser instanceof StreamParser) {
					InputStream in = null;
					try {
						in = new FileInputStream(file);
						return ((StreamParser) parser).renderText(in, context);
					}
					catch (FileNotFoundException e) {
						throw new ParseException(e);
					}
					finally {
						Closeables.closeQuietly(in);
					}
				}
				else if (parser instanceof FileParser) {
					return ((FileParser) parser).renderText(file, context);
				}
			}
			catch (OutOfMemoryError e) {
				throw new CheckedOutOfMemoryError(e);
			}
		}
		throw new IllegalArgumentException();
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
		return config.isIndexFilenames() || findParser(config, filename) != null;
	}
	
	public static boolean isBuiltInExtension(	@NotNull IndexingConfig config,
												@NotNull String extension) {
		for (Parser parser : parsers) {
			Collection<String> exts;
			if (parser == textParser)
				continue;
			else if (parser == htmlParser)
				exts = config.getHtmlExtensions();
			else
				exts = parser.getExtensions();
			for (String candidateExt : exts)
				if (candidateExt.toLowerCase().equals(extension))
					return true;
		}
		return false;
	}
	
	// accepts TrueZIP files
	@MutableCopy
	@NotNull
	@VisibleForTesting
	static List<String> getPossibleMimeTypes(@NotNull File file)
			throws IOException {
		InputStream in = null;
		try {
			in = new TFileInputStream(file);
			in = new BufferedInputStream(in); // must support mark and reset
			
			Collection<?> mimeTypes = mimeDetector.getMimeTypes(in);
			Collection<String> textTypes = textParser.getTypes();
			List<String> result = Util.createEmptyList(mimeTypes, textTypes);
			
			for (Object mimeType : mimeTypes)
				result.add(mimeType.toString().toLowerCase(Locale.ENGLISH));
			
			if (TextDetector.isText(in))
				result.addAll(textTypes);
			
			return result;
		}
		finally {
			Closeables.closeQuietly(in);
		}
	}

}
