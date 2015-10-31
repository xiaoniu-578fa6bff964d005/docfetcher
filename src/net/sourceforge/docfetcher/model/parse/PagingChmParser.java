/*******************************************************************************
 * Copyright (c) 2015 Nam-Quang Tran.
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
import java.io.FileInputStream;
import java.io.InputStream;

import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;

import org.apache.tika.parser.chm.accessor.DirectoryListingEntry;
import org.apache.tika.parser.chm.core.ChmExtractor;

public final class PagingChmParser {
	
	private final File file;
	private final PageHandler handler;

	public PagingChmParser(File file, PageHandler handler) {
		this.file = file;
		this.handler = handler;
	}
	
	public void run() throws ParseException, CheckedOutOfMemoryError {
		ChmExtractor chmExtractor = null;
		try {
			InputStream in = new FileInputStream(file);
			chmExtractor = new ChmExtractor(in);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
		
		for (DirectoryListingEntry entry : chmExtractor.getChmDirList().getDirectoryListingEntryList()) {
			String entryName = entry.getName().toLowerCase();
			if (entryName.endsWith(".html") || entryName.endsWith(".htm")) {
				try {
					byte[] data = chmExtractor.extractChmEntry(entry);
					String text = ChmParser.parsePage(data, true);
					if (text == null) {
						text = "";
					}
					if (handler.handlePage(text)) {
						return;
					}
				} catch (Exception e) {
					// Ignore
				}
			}
		}
	}

}
