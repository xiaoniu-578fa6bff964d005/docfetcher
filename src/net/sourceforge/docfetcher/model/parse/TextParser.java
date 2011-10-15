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
import java.util.Collection;
import java.util.Collections;

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.mozilla.universalchardet.UniversalDetector;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

/**
 * @author Tran Nam Quang
 */
public final class TextParser extends StreamParser {
	
	@Nullable private UniversalDetector charsetDetector;
	private final Collection<String> types = Collections.singleton("text/plain");
	
	TextParser() {
	}

	protected ParseResult parse(InputStream in,
								IndexingReporter reporter,
								Cancelable cancelable) throws ParseException {
		try {
			byte[] bytes = ByteStreams.toByteArray(in);
			
			if (charsetDetector == null)
				charsetDetector = new UniversalDetector(null);
			byte[] buf = new byte[4096];
			ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
			
			int nread;
		    while ((nread = byteIn.read(buf)) > 0 && !charsetDetector.isDone())
				charsetDetector.handleData(buf, 0, nread);
		    charsetDetector.dataEnd();
		    String charsetName = charsetDetector.getDetectedCharset();
		    charsetDetector.reset();
			
			String contents = charsetName == null ? new String(
				bytes, Charsets.ISO_8859_1) : new String(bytes, charsetName);
			
			return new ParseResult(contents);
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
	}
	
	protected Collection<String> getExtensions() {
		throw new UnsupportedOperationException();
	}
	
	protected Collection<String> getTypes() {
		return types;
	}
	
	public String getTypeLabel() {
		return "Plain Text"; // TODO i18n
	}

}
