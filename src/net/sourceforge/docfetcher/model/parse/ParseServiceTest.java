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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.parse.MSOffice2007Parser.MSWord2007Parser;
import net.sourceforge.docfetcher.model.parse.MSOfficeParser.MSPowerPointParser;
import net.sourceforge.docfetcher.model.parse.MSOfficeParser.MSWordParser;
import net.sourceforge.docfetcher.model.parse.OpenOfficeParser.OpenOfficeWriterParser;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.collect.ListMap;
import net.sourceforge.docfetcher.util.collect.ListMap.Entry;

import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class ParseServiceTest {
	
	@Test
	public void testMimeTypeDetection() throws Exception {
		ListMap<File, Parser> fileToParserMap = ListMap.<File, Parser>create()
		.add(TestFiles.lorem_ipsum_abw.get(), new AbiWordParser())
		.add(TestFiles.lorem_ipsum_abw_gz.get(), new AbiWordParser())
		.add(TestFiles.lorem_ipsum_docx.get(), new MSWord2007Parser())
		.add(TestFiles.lorem_ipsum_html.get(), new HtmlParser())
		.add(TestFiles.lorem_ipsum_odt.get(), new OpenOfficeWriterParser())
		.add(TestFiles.lorem_ipsum_pdf.get(), new PdfParser())
		.add(TestFiles.lorem_ipsum_rtf.get(), new RtfParser())
		.add(TestFiles.lorem_ipsum_svg.get(), new SvgParser())
		.add(TestFiles.lorem_ipsum_txt.get(), new TextParser())

		.add(TestFiles.lorem_ipsum_doc_97.get(), new MSWordParser())
		.add(TestFiles.lorem_ipsum_xls_5_0.get(), new MSExcelParser())
		.add(TestFiles.lorem_ipsum_xls_95.get(), new MSExcelParser())
		.add(TestFiles.lorem_ipsum_xls_97.get(), new MSExcelParser())
		.add(TestFiles.lorem_ipsum_ppt_97.get(), new MSPowerPointParser())
		;
		
		for (Entry<File, Parser> entry : fileToParserMap) {
			Collection<String> expectedTypes = entry.getValue().getTypes();
			Collection<String> actualTypes = ParseService.getPossibleMimeTypes(entry.getKey());
			assertTrue(!Collections.disjoint(expectedTypes, actualTypes));
		}
	}
	
	/**
	 * Tests that the best parser is chosen when multiple parsers support the
	 * mime type of a particular file.
	 */
	@Test
	public void testMimeTypeOverlap() throws Exception {
		IndexingConfig config = new IndexingConfig();
		
		File odtFile = TestFiles.lorem_ipsum_odt.get();
		List<Parser> odtParsers = ParseService.getSortedMatchingParsers(
			config, odtFile, odtFile.getName());
		assertTrue(equalClasses(odtParsers,
			OpenOfficeParser.OpenOfficeWriterParser.class,
			MSOffice2007Parser.MSExcel2007Parser.class,
			MSOffice2007Parser.MSPowerPoint2007Parser.class,
			MSOffice2007Parser.MSWord2007Parser.class,
			OpenOfficeParser.OpenOfficeCalcParser.class,
			OpenOfficeParser.OpenOfficeDrawParser.class,
			OpenOfficeParser.OpenOfficeImpressParser.class
		));
		
		File docxFile = TestFiles.lorem_ipsum_docx.get();
		List<Parser> docxParsers = ParseService.getSortedMatchingParsers(
			config, docxFile, docxFile.getName());
		assertTrue(equalClasses(docxParsers,
			MSOffice2007Parser.MSWord2007Parser.class,
			MSOffice2007Parser.MSExcel2007Parser.class,
			MSOffice2007Parser.MSPowerPoint2007Parser.class,
			OpenOfficeParser.OpenOfficeCalcParser.class,
			OpenOfficeParser.OpenOfficeDrawParser.class,
			OpenOfficeParser.OpenOfficeImpressParser.class,
			OpenOfficeParser.OpenOfficeWriterParser.class
		));
	}

	/**
	 * Returns true if the elements in the given collection have the classes
	 * specified in the given array of classes.
	 */
	@NotNull
	private boolean equalClasses(	@NotNull Collection<?> col,
									@NotNull Class<?>... classes) {
		if (col.size() != classes.length)
			return false;
		int i = 0;
		for (Object element : col) {
			if (!element.getClass().equals(classes[i]))
				return false;
			i++;
		}
		return true;
	}
	
	// TODO test: What happens when a parser (e.g. AbiWordParser) is fed with the wrong filetype, e.g. binary files?

}
