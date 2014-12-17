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

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * @author Tran Nam Quang
 */
public final class PdfParser extends StreamParser {
	
	private static final Collection<String> extensions = Collections.singleton("pdf");
	private static final Collection<String> types = MediaType.Col.application("pdf");
	
	PdfParser() {
	}
	
	@Override
	protected ParseResult parse(@NotNull InputStream in,
	                            @NotNull final ParseContext context)
			throws ParseException {
		PDDocument pdfDoc = null;
		try {
			/*
			 * TODO post-release-1.1: check if 'force' argument in PDDocument/Stripper increases
			 * number of parsed PDF files
			 */
			pdfDoc = PDDocument.load(in, true);
			PDDocumentInformation pdInfo;
			final int pageCount;
			try {
				pdInfo = pdfDoc.getDocumentInformation();
				pageCount = pdfDoc.getNumberOfPages();
			}
			catch (ClassCastException e) {
				// Bug #3529070 and #3528345
				throw new ParseException(e);
			}
			StringWriter writer = new StringWriter();
			final StringBuilder annotations = new StringBuilder();
			
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
					context.getReporter().subInfo(getCurrentPageNo(), pageCount);
				}
				protected void endPage(PDPage page) throws IOException {
					if (context.getCancelable().isCanceled()) {
						setEndPage(0);
						return;
					}
					for (PDAnnotation a : page.getAnnotations()) {
						if (a instanceof PDAnnotationMarkup) {
							PDAnnotationMarkup annot = (PDAnnotationMarkup) a;
							String title = annot.getTitlePopup();
							String subject = annot.getSubject();
							String contents = annot.getContents();
							if (title != null) {
								annotations.append(title + " ");
							}
							if (subject != null) {
								annotations.append(subject + " ");
							}
							if (contents != null) {
								annotations.append(contents + " ");
							}
						}
					}
				}
			};
			stripper.setForceParsing(true);
			
			try {
				stripper.writeText(pdfDoc, writer);
			}
			catch (RuntimeException e) {
				/*
				 * PDFTextStripper.writeText can throw various
				 * RuntimeExceptions, see bugs #3446010, #3448272, #3444887.
				 */
				throw new ParseException(e);
			}
			
			writer.write(" ");
			writer.write(annotations.toString());

			return new ParseResult(writer.getBuffer()).setTitle(
				pdInfo.getTitle())
					.addAuthor(pdInfo.getAuthor())
					.addMiscMetadata(pdInfo.getSubject())
					.addMiscMetadata(pdInfo.getKeywords());
		}
		catch (IOException e) {
			if (e.getCause() instanceof CryptographyException)
				throw new ParseException(Msg.doc_pw_protected.get());
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
		return Msg.filetype_pdf.get();
	}

}
