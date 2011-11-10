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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
final class AbiWordParser extends StreamParser {
	
	private static final Collection<String> extensions = Arrays.asList(
		"abw", "abw.gz", "zabw");
	private static final Collection<String> types = Arrays.asList(
		MediaType.text("xml"),
		MediaType.application("x-gzip"));

	@Override
	protected ParseResult parse(InputStream in,
	                            ParseContext context) throws ParseException {
		Source source = getSource(in, context.getFilename());
		String author = getMetaData(source, "dc.creator"); //$NON-NLS-1$
		String title = getMetaData(source, "dc.title"); //$NON-NLS-1$
		String contents = source.getTextExtractor().toString(); // Includes metadata
		return new ParseResult(contents).setTitle(title).addAuthor(author);
	}
	
	/**
	 * Returns the value of the given metadata key in the given {@code Source},
	 * or null if the key-value-pair was not found.
	 */
	@Nullable
	private String getMetaData(@NotNull Source source, @NotNull String key) {
		Element metaElement = source.getNextElement(0, "key", key, false); //$NON-NLS-1$
		if (metaElement == null)
			return null;
		return metaElement.getTextExtractor().toString();
	}
	
	@Override
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		Source source = getSource(in, filename);
		
		// Find all top level elements, excluding the metadata element
		List<Element> topLevelNonMetaElements = new ArrayList<Element>();
		int pos = source.getNextElement(0, "metadata").getEnd(); //$NON-NLS-1$
		while (pos < source.length()) {
			Element next = source.getNextElement(pos);
			if (next == null)
				break;
			topLevelNonMetaElements.add(next);
			pos = next.getEnd();
		}
		
		// Invoke renderer on all found elements, save output to stringbuffer
		StringBuilder sb = new StringBuilder();
		for (Element element : topLevelNonMetaElements)
			sb.append(element.getRenderer().toString());
		
		return sb.toString();
	}
	
	/**
	 * Returns a {@code Source} for the given AbiWord file.
	 */
	@NotNull
	private static Source getSource(@NotNull InputStream in,
									@NotNull String filename)
			throws ParseException {
		try {
			String ext = Util.getExtension(filename);
			if (ext.equals("zabw") || ext.equals("abw.gz")) //$NON-NLS-1$ //$NON-NLS-2$
				in = new GZIPInputStream(in);
			Source source = new Source(in);
			source.setLogger(null);
			source.fullSequentialParse();
			return source;
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
		finally {
			Closeables.closeQuietly(in);
		}
	}

	protected Collection<String> getExtensions() {
		return extensions;
	}

	protected Collection<String> getTypes() {
		return types;
	}

	public String getTypeLabel() {
		return Msg.filetype_abi.get();
	}

}
