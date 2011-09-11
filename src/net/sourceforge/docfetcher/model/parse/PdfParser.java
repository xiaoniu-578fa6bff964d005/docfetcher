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
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.index.IndexingReporter;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * @author Tran Nam Quang
 */
public final class PdfParser extends StreamParser {
	
	private static final Collection<String> extensions = Collections.singleton("pdf");
	private static final Collection<String> types = Collections.singleton(MediaType.application("pdf"));
	
	PdfParser() {
	}
	
	protected ParseResult parse(@NotNull InputStream in,
	                            @NotNull final IndexingReporter reporter,
								@NotNull final Cancelable cancelable)
			throws ParseException {
		PDDocument pdfDoc = null;
		try {
			/*
			 * TODO check if 'force' argument in PDDocument/Stripper increases
			 * number of parsed PDF files
			 * 
			 * TODO maybe write unit test that checks that the default values
			 * for force-parsing and sort-by-position are both false.
			 */
			pdfDoc = PDDocument.load(in, true);
			PDDocumentInformation pdInfo = pdfDoc.getDocumentInformation();
			final int pageCount = pdfDoc.getNumberOfPages();
			StringWriter writer = new StringWriter();
			
			/*
			 * If the PDF file is encrypted, the PDF stripper will automatically
			 * try an empty password.
			 * 
			 * In contrast to the paging PDF parser that is used for the
			 * preview, we do not need to call setSortByPosition(true) here
			 * because the extracted text will be digested by Lucene anyway.
			 */
			PDFTextStripper stripper = new PDFTextStripper() {
				protected void startPage(PDPage page) throws IOException {
					reporter.subInfo(getCurrentPageNo(), pageCount);
				}
				protected void endPage(PDPage page) throws IOException {
					if (cancelable.isCanceled())
						setEndPage(0);
				}
			};
			stripper.setForceParsing(true);
			stripper.writeText(pdfDoc, writer);

			return new ParseResult(writer.getBuffer()).setTitle(
				pdInfo.getTitle())
					.addAuthor(pdInfo.getAuthor())
					.addMiscMetadata(pdInfo.getSubject())
					.addMiscMetadata(pdInfo.getKeywords());
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
		finally {
			close(pdfDoc);
		}
	}
	
	static void close(@Nullable PDDocument doc) {
		if (doc != null) {
			try {
				doc.close();
			}
			catch (IOException e) {
			}
		}
	}
	
	protected Collection<String> getExtensions() {
		return extensions;
	}
	
	protected Collection<String> getTypes() {
		return types;
	}
	
	public String getTypeLabel() {
		return "PDF"; // TODO
	}

}
