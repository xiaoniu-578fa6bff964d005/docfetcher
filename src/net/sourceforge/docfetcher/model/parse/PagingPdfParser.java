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

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * @author Tran Nam Quang
 */
public abstract class PagingPdfParser {
	
	private final File file;
	private final StringWriter writer = new StringWriter();
	private boolean stopped = false;

	public PagingPdfParser(@NotNull File file) {
		this.file = Util.checkNotNull(file);
	}
	
	public final void run() throws ParseException {
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
		finally {
			PdfParser.close(doc);
		}
	}

	public final void stop() {
		stopped = true;
	}
	
	protected abstract void handlePage(@NotNull String pageText);
	
	private class PagingStripper extends PDFTextStripper {
		public PagingStripper() throws IOException {
			super();
		}

		protected void endPage(PDPage page) throws IOException {
			StringBuffer buffer = writer.getBuffer();
			handlePage(buffer.toString());
			buffer.delete(0, buffer.length());
			if (stopped)
				setEndPage(0);
		}
	}

}
