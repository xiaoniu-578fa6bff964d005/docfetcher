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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
final class MSExcelParser extends MSOfficeParser {
	
	public MSExcelParser() {
		super(Msg.filetype_xls.get(), "xls", "xlt");
	}

	@Override
	protected String extractText(InputStream in) throws IOException {
		throw new UnsupportedOperationException();
	}

	protected String renderText(File file, String filename)
			throws ParseException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			ExcelExtractor extractor = null;
			try {
				POIFSFileSystem fs = new POIFSFileSystem(in);
				extractor = new ExcelExtractor(fs);
				extractor.setFormulasNotResults(ProgramConf.Bool.IndexExcelFormulas.get());
				return extractor.getText();
			}
			catch (OldExcelFormatException e) {
				/*
				 * POI doesn't support the old Excel 5.0/7.0 (BIFF5) format,
				 * only the BIFF8 format from Excel 97/2000/XP/2003. Thus, we
				 * fall back to another Excel library.
				 */
				Closeables.closeQuietly(in);
				return extractWithJexcelAPI(file);
			}
			finally {
				Closeables.closeQuietly(extractor);
			}
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
		catch (RuntimeException e) {
			// POI can throw NullPointerExceptions on some odd Excel files
			throw new ParseException(e);
		}
		finally {
			Closeables.closeQuietly(in);
		}
	}

	@NotNull
	private String extractWithJexcelAPI(@NotNull File file)
			throws ParseException {
		WorkbookSettings wbSettings = new WorkbookSettings();
		wbSettings.setSuppressWarnings(true);
		Workbook workbook = null;
		try {
			workbook = Workbook.getWorkbook(file, wbSettings);
			StringBuilder sb = new StringBuilder();
			for (int sIndex = 0; sIndex < workbook.getNumberOfSheets(); sIndex++) {
				Sheet sheet = workbook.getSheet(sIndex);
				sb.append(sheet.getName()).append("\n\n"); //$NON-NLS-1$
				for (int i = 0; i < sheet.getRows(); i++) {
					Cell[] row = sheet.getRow(i);
					for (int j = 0; j < row.length; j++)
						sb.append(row[j].getContents()).append(" "); //$NON-NLS-1$
					sb.append("\n");
				}
				sb.append("\n\n\n"); //$NON-NLS-1$
			}
			return sb.toString();
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}
	}
	
}