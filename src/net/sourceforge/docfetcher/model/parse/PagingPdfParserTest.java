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

import java.util.ArrayList;
import java.util.List;


import net.sourceforge.docfetcher.TestFiles;
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
		new PagingPdfParser(TestFiles.multi_page_pdf.get()) {
			protected void handlePage(String pageText) {
				pages.add(pageText);
			}
		}.run();
		assertEquals(3, pages.size());
		assertEquals("page 1" + Util.LS, pages.get(0));
		assertEquals("page 2" + Util.LS, pages.get(1));
		assertEquals("page 3" + Util.LS, pages.get(2));
	}
	
	@Test
	public void testParseAndStop() throws ParseException,
			CheckedOutOfMemoryError {
		final List<String> pages = new ArrayList<String> (2);
		new PagingPdfParser(TestFiles.multi_page_pdf.get()) {
			protected void handlePage(String pageText) {
				pages.add(pageText);
				if (pages.size() >= 2) stop();
			}
		}.run();
		assertEquals(2, pages.size());
		assertEquals("page 1" + Util.LS, pages.get(0));
		assertEquals("page 2" + Util.LS, pages.get(1));
	}

}
