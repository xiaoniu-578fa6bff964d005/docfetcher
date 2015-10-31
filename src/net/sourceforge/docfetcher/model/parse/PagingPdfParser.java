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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * @author Tran Nam Quang
 */
public final class PagingPdfParser {
	
	private final File file;
	private final PageHandler handler;
	private final StringWriter writer = new StringWriter();

	public PagingPdfParser(File file, PageHandler handler) {
		this.file = file;
		this.handler = handler;
	}
	
	public void run() throws ParseException, CheckedOutOfMemoryError {
		PDDocument doc = null;
		try {
			doc = PDDocument.load(file);
			PagingStripper stripper = new PagingStripper();
			stripper.setForceParsing(true);
			stripper.setSortByPosition(true);
			stripper.writeText(doc, writer);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
		catch (OutOfMemoryError e) {
			throw new CheckedOutOfMemoryError(e);
		}
		finally {
			PdfParser.close(doc);
		}
	}

	private class PagingStripper extends PDFTextStripper {
		public PagingStripper() throws IOException {
			super();
		}

		protected void endPage(PDPage page) throws IOException {
			StringBuffer buffer = writer.getBuffer();
			boolean stopped = handler.handlePage(buffer.toString());
			buffer.delete(0, buffer.length());
			if (stopped)
				setEndPage(0);
		}
	}

}
