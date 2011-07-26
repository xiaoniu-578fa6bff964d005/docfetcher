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
import java.util.Collection;
import java.util.Collections;

import net.sourceforge.docfetcher.model.Cancelable;

import com.google.common.io.ByteStreams;

/**
 * @author Tran Nam Quang
 */
final class TextParser extends StreamParser {
	
	private final Collection<String> types = Collections.singleton("text/plain");

	protected ParseResult parse(InputStream in, Cancelable cancelable)
			throws ParseException {
		// TODO try to detect charset -> see Tika text parser
		try {
			byte[] bytes = ByteStreams.toByteArray(in);
			String contents = new String(bytes, "utf-8");
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
		return "Plain Text"; // TODO
	}

}
