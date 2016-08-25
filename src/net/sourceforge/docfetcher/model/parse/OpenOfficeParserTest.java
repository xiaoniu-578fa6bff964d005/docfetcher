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

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.model.parse.OpenOfficeParser.OpenOfficeWriterParser;

import org.junit.Test;

import de.schlichtherle.truezip.file.TFile;

public class OpenOfficeParserTest {
	
	@Test
	public void testPasswordProtected() throws Exception {
		FileParser parser = new OpenOfficeWriterParser();
		File file = TestFiles.encrypted_odt.get();
		try {
			parser.parse(file, new ParseContext(file.getName()));
		}
		catch (ParseException e) {
			if (e.getMessage().equals(Msg.doc_pw_protected.get())) {
				return;
			}
		}
		throw new IllegalStateException();
	}
	
	@Test
	public void testNotPasswordProtected() throws Exception {
		FileParser parser = new OpenOfficeWriterParser();
		File[] files = new File[] {
			TestFiles.lorem_ipsum_odt.get(),
			new TFile(TestFiles.lorem_ipsum_odt.get())
		};
		for (File file : files) {
			parser.parse(file, new ParseContext(file.getName()));
		}
	}
	
	@Test
	public void testMetaXmlMissing() throws Exception {
		FileParser parser = new OpenOfficeWriterParser();
		File file = TestFiles.missing_meta_xml_entry.get();
		try {
			parser.parse(file, new ParseContext(file.getName()));
		}
		catch (ParseException e) {
			if ("entry".equals(e.getCause().getMessage())) {
				return;
			}
		}
		throw new IllegalStateException();
	}

}
