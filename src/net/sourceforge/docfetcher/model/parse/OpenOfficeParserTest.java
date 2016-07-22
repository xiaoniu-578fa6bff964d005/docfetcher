/*******************************************************************************
 * Copyright (c) 2016 Nam-Quang Tran.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nam-Quang Tran - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.model.parse.OpenOfficeParser.OpenOfficeWriterParser;

import org.junit.Test;

import com.google.common.io.Closeables;

public class OpenOfficeParserTest {
	
	@Test
	public void testPasswordProtected() throws Exception {
		StreamParser parser = new OpenOfficeWriterParser();
		File file = TestFiles.encrypted_odt.get();
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			parser.parse(in, new ParseContext(file.getName()));
		}
		catch (ParseException e) {
			if (e.getMessage().equals(Msg.doc_pw_protected.get())) {
				return;
			}
		}
		finally {
			Closeables.closeQuietly(in);
		}
		throw new IllegalStateException();
	}

}
