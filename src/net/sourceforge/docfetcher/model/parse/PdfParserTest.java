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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Logger;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.model.index.IndexingConfig;

import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class PdfParserTest {
	
	@Test
	public void testEncryptedPdf() throws Exception {
		Logger logger = Logger.getLogger("org.apache.pdfbox.pdfparser.PDFParser");
		logger.setLevel(java.util.logging.Level.OFF);
		try {
			File file = TestFiles.encrypted_pdf.get();
			ParseService.renderText(
				new IndexingConfig(), file, file.getName(),
				PdfParser.class.getSimpleName());
			assertTrue(false);
		}
		catch (ParseException e) {
			assertTrue(e.getMessage().equals(Msg.doc_pw_protected.get()));
		}
	}

}
