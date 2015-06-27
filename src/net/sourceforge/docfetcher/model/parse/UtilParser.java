/*******************************************************************************
 * Copyright (c) 2012 Tran Nam Quang.
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
import java.io.Reader;
import java.io.StringReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.CharsetDetectorHelper;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
final class UtilParser {
	
	private UtilParser() {
	}
	
	// does charset detection
	// does not close the given InputStream
	public static Source getSource(InputStream in) throws IOException {
		Reader reader = new StringReader(CharsetDetectorHelper.toString(in));
		Source source = new Source(reader);
		return source;
	}
	
	public static Source getSource(ZipFile file, String entryPath) throws IOException, ParseException {
		ZipEntry entry = file.getEntry(entryPath);
		if (entry == null) {
			// Apparently, ZipFile.getEntry expects forward slashes even on Windows
			entry = file.getEntry(entryPath.replace("\\", "/"));
			if (entry == null) {
				throw new ParseException(Msg.file_corrupted.get());
			}
		}
		InputStream inputStream = file.getInputStream(entry);
		Source source = getSource(inputStream);
		Closeables.closeQuietly(inputStream);
		source.setLogger(null);
		return source;
	}
	
	@Nullable
	public static String extract(@Nullable Element e) {
		if (e == null) {
			return null;
		}
		return e.getContent().getTextExtractor().toString();
	}
	
	public static String render(Segment e) {
		return e.getRenderer().setIncludeHyperlinkURLs(false).toString();
	}
	
	public static void closeZipFile(@Nullable ZipFile zipFile) {
		// We can't use Closeables.closeQuietly for ZipFiles because it doesn't
		// implement the Closeable interface on Mac OS X.
		if (zipFile == null)
			return;
		try {
			zipFile.close();
		}
		catch (IOException e) {
		}
	}
	
}
