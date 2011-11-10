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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;

import net.htmlparser.jericho.Source;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.chm4j.ChmEntry;
import org.chm4j.ChmFile;

import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
final class ChmParser extends FileParser {

	/*
	 * TODO post-release-1.1: replace this with Tika's CHM parser. Then:
	 * - remove chm4j dependency
	 * - enable CHM parser on Mac
	 * - throw runtime exception when ParseService.findParser can't find a suitable parser
	 */
	
	private static final Collection<String> extensions = Collections.singleton("chm");
	
	@Override
	protected ParseResult parse(File file,
								ParseContext context) throws ParseException {
		return new ParseResult(renderText(file, false));
	}
	
	@Override
	protected String renderText(File file, String filename)
			throws ParseException {
		return renderText(file, true);
	}
	
	@NotNull
	private String renderText(@NotNull File file, boolean renderText)
			throws ParseException {
		StringBuilder contents = new StringBuilder();
		try {
			ChmFile chmFile = new ChmFile(file);
			ChmEntry[] entries = chmFile.entries(ChmEntry.Attribute.ALL);
			for (ChmEntry entry : entries)
				append(contents, entry, renderText);
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
		return contents.toString();
	}
	
	/**
	 * Converts all <tt>ChmEntry</tt>s under <tt>entry</tt> to strings and
	 * puts them into the given <tt>StringBuilder</tt>.
	 * 
	 * @param renderText
	 *            Whether the textual contents of the <tt>ChmEntry</tt>s
	 *            should be extracted in a readable format (true) or as raw
	 *            strings (false).
	 */
	private void append(@NotNull StringBuilder sb,
						@NotNull ChmEntry entry,
						boolean renderText) throws IOException {
		if (entry.hasAttribute(ChmEntry.Attribute.DIRECTORY)) {
			for (ChmEntry child : entry.entries(ChmEntry.Attribute.ALL))
				append(sb, child, renderText);
		}
		else {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(
						new InputStreamReader(
								entry.getInputStream(),
								"utf8" // Just guessing... //$NON-NLS-1$
						)
				);
				StringBuilder entryBuffer = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null)
					entryBuffer.append(line).append("\n\n"); //$NON-NLS-1$

				/*
				 * The current version of chm4j doesn't allow differentiating
				 * between binary files (such as images) and HTML files. Therefore
				 * we scan the text for HTML tags to select the HTML files.
				 */
				if (isHTML(entryBuffer)) {
					Source source = new Source(entryBuffer);
					source.setLogger(null);
					if (renderText)
						sb.append(source.getRenderer().setIncludeHyperlinkURLs(false).toString());
					else
						sb.append(source.getTextExtractor().toString());
				}
			}
			catch (RuntimeException e) {
				// The HTML lib can do this to us; do nothing
			}
			finally {
				Closeables.closeQuietly(reader);
			}
		}
	}
	
	/**
	 * Returns true if the given StringBuilder appears to contain HTML. This is
	 * determined by parsing the input with a simple finite state machine that
	 * checks whether the input contains an html start tag, followed by an html
	 * end tag.
	 * <p>
	 * Note: This is better than using regular expressions because the latter
	 * can crash the program with a StackOverflowError, as seen in bug report
	 * #2948903.
	 */
	private static boolean isHTML(@NotNull StringBuilder input) {
		final int OUTSIDE = 0;
		final int INSIDE = 1;
		int state = OUTSIDE;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (state == OUTSIDE) {
				/*
				 * Note that we're checking for the occurrence of <html, not
				 * <html>, since in some HTML documents the html start tag
				 * contains additional attributes, e.g. <html attr="value">.
				 */
				if (c == 'l' || c == 'L') { // last char in 'html'
					if (i >= 4) {
						String substring = input.substring(i - 4, i + 1);
						if (substring.toLowerCase().equals("<html"))
							state = INSIDE;
					}
				}
			}
			else if (state == INSIDE) {
				if (c == '>') {
					if (i >= 6) {
						String substring = input.substring(i - 6, i + 1);
						if (substring.toLowerCase().equals("</html>"))
							return true;
					}
				}
			}
		}
		return false;
	}

	protected Collection<String> getExtensions() {
		return extensions;
	}

	protected Collection<String> getTypes() {
		/*
		 * The mime-util library doesn't seem to be able to detect CHM files, so
		 * we'll return an empty list here.
		 */
		return Collections.emptyList();
	}
	
	public String getTypeLabel() {
		return Msg.filetype_chm.get();
	}

}
