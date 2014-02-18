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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.model.index.PatternAction.MatchAction;
import net.sourceforge.docfetcher.model.parse.MSOffice2007Parser.MSExcel2007Parser;
import net.sourceforge.docfetcher.model.parse.MSOffice2007Parser.MSPowerPoint2007Parser;
import net.sourceforge.docfetcher.model.parse.MSOffice2007Parser.MSWord2007Parser;
import net.sourceforge.docfetcher.model.parse.MSOfficeParser.MSPowerPointParser;
import net.sourceforge.docfetcher.model.parse.MSOfficeParser.MSVisioParser;
import net.sourceforge.docfetcher.model.parse.MSOfficeParser.MSWordParser;
import net.sourceforge.docfetcher.model.parse.OpenOfficeParser.OpenOfficeCalcParser;
import net.sourceforge.docfetcher.model.parse.OpenOfficeParser.OpenOfficeDrawParser;
import net.sourceforge.docfetcher.model.parse.OpenOfficeParser.OpenOfficeImpressParser;
import net.sourceforge.docfetcher.model.parse.OpenOfficeParser.OpenOfficeWriterParser;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.primitives.Ints;

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
	
	private static final List<Parser> parsers = Util.createList(1,
		// TextParser must have high priority since its file extensions can be customized
		textParser = new TextParser(),
		htmlParser = new HtmlParser(),
		
		new ExifParser(),
		new MP3Parser(),
		new FLACParser(),
		new AbiWordParser(),
		new PdfParser(),
		new RtfParser(),
		new SvgParser(),
		new EpubParser(),
		
		new OpenOfficeWriterParser(),
		new OpenOfficeCalcParser(),
		new OpenOfficeDrawParser(),
		new OpenOfficeImpressParser(),
		
		new MSWordParser(),
		new MSExcelParser(),
		new MSPowerPointParser(),
		new MSVisioParser(),
		
		new MSWord2007Parser(),
		new MSExcel2007Parser(),
		new MSPowerPoint2007Parser()
	);
	
	static {
		if (!Util.IS_MAC_OS_X && !Util.IS_64_BIT_JVM)
			parsers.add(new ChmParser());
	}

	private ParseService() {}
	
	@Immutable
	@NotNull
	public static List<Parser> getParsers() {
		return parsers;
	}

	/**
	 * Returns a list containing all parsers that support the mime type and/or
	 * file extension of the given file.
	 * <p>
	 * The list is sorted by the degree of matching: If a parser supports
	 * <em>both</em> the mime type <em>and</em> the file extension of the given
	 * file, it appears in the returned list before other parsers that support
	 * either only the mime type or only the file extension.
	 */
	@MutableCopy
	@NotNull
	@VisibleForTesting
	static List<Parser> getSortedMatchingParsers(	@NotNull IndexingConfig config,
													@NotNull File file,
													@NotNull String filename)
			throws IOException {
		class Match {
			final Parser parser;
			boolean mimeMatch = false;
			boolean extMatch = false;
			
			public Match(@NotNull Parser parser) {
				this.parser = parser;
			}
			public int getMatchCount() {
				return (mimeMatch ? 1 : 0) + (extMatch ? 1 : 0);
			}
		}
		
		Set<Match> matches = Sets.newTreeSet(new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				// Element with higher match count comes first
				int cmp = -1 * Ints.compare(m1.getMatchCount(), m2.getMatchCount());
				if (cmp != 0)
					return cmp;
				
				// Element with mime match comes before element with file
				// extension match
				if (m1.mimeMatch && !m2.mimeMatch)
					return -1;
				if (!m1.mimeMatch && m2.mimeMatch)
					return 1;
				
				// Compare parser names
				String name1 = m1.parser.getClass().getSimpleName();
				String name2 = m2.parser.getClass().getSimpleName();
				return name1.compareTo(name2);
			}
		});
		
		List<String> mimeTypes = getPossibleMimeTypes(file);
		String ext = Util.getExtension(filename);
		
		for (Parser parser : parsers) {
			Match match = new Match(parser);
			if (!Collections.disjoint(mimeTypes, parser.getTypes()))
				match.mimeMatch = true;
			for (String candidateExt : getExtensions(config, parser)) {
				if (candidateExt.toLowerCase().equals(ext)) {
					match.extMatch = true;
					break;
				}
			}
			if (match.mimeMatch || match.extMatch)
				matches.add(match);
		}
		
		List<Parser> parsers = Util.createEmptyList(matches);
		for (Match match : matches)
			parsers.add(match.parser);
		return parsers;
	}
	
	// Accepts HTML files, but does not handle HTML pairing
	// When parsing a temporary file and mime type detection is off, the temporary file
	// must have the correct file extension.
	// Accepts TrueZIP files
	@NotNull
	public static ParseResult parse(@NotNull IndexingConfig config,
	                                @NotNull File file,
	                                @NotNull String filename,
	                                @NotNull Path filepath,
	                                @NotNull IndexingReporter reporter,
	                                @NotNull Cancelable cancelable)
			throws ParseException, CheckedOutOfMemoryError {
		ParseContext context = new ParseContext(filename, reporter, cancelable);
		
		// Search for appropriate parser by mimetype
		for (PatternAction patternAction : config.getPatternActions()) {
			if (patternAction.getAction() != MatchAction.DETECT_MIME)
				continue;
			if (!patternAction.matches(filename, filepath, true))
				continue;
			try {
				List<Parser> matchingParsers = getSortedMatchingParsers(
					config, file, filename);
				for (Parser parser : matchingParsers) {
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
		Parser parser = findParserByName(config, file.getName());
		if (parser != null)
			return doParse(config, parser, file, context);
		
		/*
		 * Fall back to filename parser if allowed. The filename will be added
		 * to the contents later.
		 */
		if (config.isIndexFilenames())
			return new ParseResult("").setParserName(FILENAME_PARSER);
		
		throw new ParseException(Msg.parser_not_found.get());
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
					catch (RuntimeException e) {
						/*
						 * Bug #408: We'll get an InvalidPathException if we try
						 * to unpack a file whose name contains a character that
						 * is not valid on the current platform. For example,
						 * the user could create a file with a colon (':') in
						 * its name on Linux, put this file in an archive, and
						 * then try to index the archive on Windows. - The colon
						 * character is supported on Linux, but not on Windows.
						 */
						throw new ParseException(e);
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
	                                @NotNull String filename,
									@NotNull String parserName)
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
						return ((StreamParser) parser).renderText(in, filename);
					}
					catch (FileNotFoundException e) {
						throw new ParseException(e);
					}
					finally {
						Closeables.closeQuietly(in);
					}
				}
				else if (parser instanceof FileParser) {
					return ((FileParser) parser).renderText(file, filename);
				}
			}
			catch (OutOfMemoryError e) {
				throw new CheckedOutOfMemoryError(e);
			}
		}
		/*
		 * This could happen when somebody indexes a CHM file on Windows/Linux,
		 * then moves DocFetcher to Mac OS X and tries to view the indexed CHM
		 * file in the preview panel.
		 */
		throw new ParseException(Msg.parser_not_found.get());
//		throw new IllegalArgumentException();
	}
	
	@Nullable
	private static Parser findParserByName(	@NotNull IndexingConfig config,
											@NotNull String filename) {
		String ext = Util.getExtension(filename);
		for (Parser parser : parsers)
			for (String candidateExt : getExtensions(config, parser))
				if (candidateExt.toLowerCase().equals(ext))
					return parser;
		return null;
	}

	@Immutable
	@NotNull
	private static Collection<String> getExtensions(@NotNull IndexingConfig config,
													@NotNull Parser parser) {
		if (parser == textParser)
			return config.getTextExtensions();
		else if (parser == htmlParser)
			return config.getHtmlExtensions();
		return parser.getExtensions();
	}
	
	public static boolean canParseByName(	@NotNull IndexingConfig config,
											@NotNull String filename) {
		return config.isIndexFilenames()
				|| findParserByName(config, filename) != null;
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
