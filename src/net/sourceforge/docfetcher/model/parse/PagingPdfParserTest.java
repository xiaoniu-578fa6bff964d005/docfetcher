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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;

import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class PagingPdfParserTest {
	
	@Test
	public void testParse() throws ParseException, CheckedOutOfMemoryError {
		final List<String> pages = new ArrayList<String> (3);
		PageHandler handler = new PageHandler() {
			public boolean handlePage(String pageText) {
				pages.add(pageText);
				return false;
			}
		};
		new PagingPdfParser(TestFiles.multi_page_pdf.get(), handler).run();
		assertEquals(3, pages.size());
		assertEquals("page 1" + Util.LS, pages.get(0));
		assertEquals("page 2" + Util.LS, pages.get(1));
		assertEquals("page 3" + Util.LS, pages.get(2));
	}
	
	@Test
	public void testParseAndStop() throws ParseException,
			CheckedOutOfMemoryError {
		final List<String> pages = new ArrayList<String> (2);
		PageHandler handler = new PageHandler() {
			public boolean handlePage(String pageText) {
				pages.add(pageText);
				return pages.size() >= 2;
			}
		};
		new PagingPdfParser(TestFiles.multi_page_pdf.get(), handler).run();
		assertEquals(2, pages.size());
		assertEquals("page 1" + Util.LS, pages.get(0));
		assertEquals("page 2" + Util.LS, pages.get(1));
	}
	
	@Test
	public void testEncryptedPdf() throws Exception {
		Logger logger = Logger.getLogger("org.apache.pdfbox.pdfparser.PDFParser");
		logger.setLevel(java.util.logging.Level.OFF);
		try {
			PageHandler handler = new PageHandler() {
				public boolean handlePage(String pageText) {
					return false;
				}
			};
			new PagingPdfParser(TestFiles.encrypted_pdf.get(), handler).run();
			assertTrue(false);
		}
		catch (ParseException e) {
			assertTrue(e.getMessage().equals(Msg.doc_pw_protected.get()));
		}
	}

}
