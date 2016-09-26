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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.htmlparser.jericho.Source;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.apache.tika.parser.chm.accessor.ChmDirectoryListingSet;
import org.apache.tika.parser.chm.accessor.DirectoryListingEntry;
import org.apache.tika.parser.chm.core.ChmExtractor;

/**
 * @author Tran Nam Quang
 */
public final class ChmParser extends StreamParser {

	private static final Collection<String> extensions = Collections.singleton("chm");
	
	@Override
	protected ParseResult parse(InputStream in,
								ParseContext context) throws ParseException {
		ChmExtractor chmExtractor = null;
		try {
			chmExtractor = new ChmExtractor(in);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}

		List<DirectoryListingEntry> htmlEntries = new ArrayList<DirectoryListingEntry>();
		ChmDirectoryListingSet chmDirList = chmExtractor.getChmDirList();
		if (chmDirList == null) { // Bug #1232
			throw new ParseException("Failed to list entries.");
		}
		for (DirectoryListingEntry entry : chmDirList.getDirectoryListingEntryList()) {
			String entryName = entry.getName().toLowerCase();
			if (entryName.endsWith(".html") || entryName.endsWith(".htm")) {
				htmlEntries.add(entry);
			}
		}

		StringBuilder contents = new StringBuilder();
		int pageCount = htmlEntries.size();
		int pageNo = 1;
		for (DirectoryListingEntry entry : htmlEntries) {
			context.getReporter().subInfo(pageNo, pageCount);
			try {
				byte[] data = chmExtractor.extractChmEntry(entry);
				String text = parsePage(data, false);
				if (text != null && !text.isEmpty()) {
					if (pageNo > 1) {
						contents.append("\n\n");
					}
					contents.append(text);
				}
			} catch (Exception e) {
				// Ignore
			}
			pageNo += 1;
		}
		return new ParseResult(contents.toString());
	}
	
	@Override
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		ChmExtractor chmExtractor = null;
		try {
			chmExtractor = new ChmExtractor(in);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
		StringBuilder contents = new StringBuilder();
		for (DirectoryListingEntry entry : chmExtractor.getChmDirList().getDirectoryListingEntryList()) {
			String entryName = entry.getName().toLowerCase();
			if (entryName.endsWith(".html") || entryName.endsWith(".htm")) {
				try {
					byte[] data = chmExtractor.extractChmEntry(entry);
					String text = parsePage(data, true);
					if (text != null && !text.isEmpty()) {
						if (contents.length() > 0) {
							contents.append("\n\n");
						}
						contents.append(text);
					}
				} catch (Exception e) {
					// Ignore
				}
			}
		}
		return contents.toString();
	}
	
	/**
	 * @param renderText
	 *            Whether the textual contents of the <tt>ChmEntry</tt>s
	 *            should be extracted in a readable format (true) or as raw
	 *            strings (false).
	 */
	@Nullable
	static String parsePage(byte[] data, boolean renderText) throws IOException {
		try {
			Source source = new Source(new ByteArrayInputStream(data));
			source.setLogger(null);
			if (renderText)
				return source.getRenderer().setIncludeHyperlinkURLs(false).toString();
			else
				return source.getTextExtractor().toString();
		}
		catch (RuntimeException e) {
			// The HTML lib can do this to us; do nothing
			return null;
		}
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
