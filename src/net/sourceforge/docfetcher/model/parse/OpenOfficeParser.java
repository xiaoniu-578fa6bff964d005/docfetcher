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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.catcode.odf.OpenDocumentTextInputStream;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
abstract class OpenOfficeParser extends FileParser {
	
	public static final class OpenOfficeWriterParser extends OpenOfficeParser {
		public OpenOfficeWriterParser() {
			super(Msg.filetype_odt.get(), "odt", "ott");
		}
	}
	
	public static final class OpenOfficeCalcParser extends OpenOfficeParser {
		public OpenOfficeCalcParser() {
			super(Msg.filetype_ods.get(), "ods", "ots");
		}
	}
	
	public static final class OpenOfficeDrawParser extends OpenOfficeParser {
		public OpenOfficeDrawParser() {
			super(Msg.filetype_odg.get(), "odg", "otg");
		}
	}
	
	public static final class OpenOfficeImpressParser extends OpenOfficeParser {
		public OpenOfficeImpressParser() {
			super(Msg.filetype_odp.get(), "odp", "otp");
		}
	}
	
	private static final Collection<String> types = MediaType.Col.application("zip");
	
	private final String typeLabel;
	private final Collection<String> extensions;
	
	private OpenOfficeParser(@NotNull String typeLabel, @NotNull String... extensions) {
		this.typeLabel = typeLabel;
		this.extensions = Arrays.asList(extensions);
	}
	
	protected final Collection<String> getExtensions() {
		return extensions;
	}
	
	public final String getTypeLabel() {
		return typeLabel;
	}
	
	protected final Collection<String> getTypes() {
		return types;
	}
	
	@Override
	protected final ParseResult parse(	@NotNull File file,
										@NotNull ParseContext context)
			throws ParseException {
		ZipFile zipFile = null;
		try {
			// Get zip entries
			zipFile = new ZipFile(file);
			
			// Find out if file is password protected
			Source manifSource = UtilParser.getSource(zipFile, "META-INF/manifest.xml"); //$NON-NLS-1$
			StartTag encryptTag = manifSource.getNextStartTag(0, "manifest:encryption-data"); //$NON-NLS-1$
			if (encryptTag != null)
				throw new ParseException(Msg.doc_pw_protected.get());
			
			// Get tags from meta.xml file
			Source metaSource = UtilParser.getSource(zipFile, "meta.xml"); //$NON-NLS-1$
			String title = getElementContent(metaSource, "dc:title"); //$NON-NLS-1$
			String author = getElementContent(metaSource, "dc:creator"); //$NON-NLS-1$
			String description = getElementContent(metaSource, "dc:description"); //$NON-NLS-1$
			String subject = getElementContent(metaSource, "dc:subject"); //$NON-NLS-1$
			String keyword = getElementContent(metaSource, "meta:keyword"); //$NON-NLS-1$

			// Collect content.xml entries
			List<ZipEntry> contentEntries = new ArrayList<ZipEntry>();
			ZipEntry contentZipEntry = zipFile.getEntry("content.xml"); //$NON-NLS-1$
			contentEntries.add(contentZipEntry);
			Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry entry = zipEntries.nextElement();
				if (entry.getName().endsWith("/content.xml")) //$NON-NLS-1$
					contentEntries.add(entry);
			}
			
			// Get contents from the content.xml entries
			StringBuilder sb = new StringBuilder();
			for (ZipEntry entry : contentEntries) {
				InputStream contentInputStream = zipFile.getInputStream(entry);
				Source contentSource = new Source(contentInputStream);
				Closeables.closeQuietly(contentInputStream);
				contentSource.setLogger(null);
				Element contentElement = contentSource.getNextElement(0, "office:body"); //$NON-NLS-1$
				if (contentElement == null) // this content.xml file doesn't seem to contain text
					continue;
				String content = UtilParser.extract(contentElement);
				sb.append(content).append(" "); //$NON-NLS-1$
			}
			
			// Create and return parse result
			return new ParseResult(sb)
				.setTitle(title)
				.addAuthor(author)
				.addMiscMetadata(description)
				.addMiscMetadata(subject)
				.addMiscMetadata(keyword);
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
		finally {
			UtilParser.closeZipFile(zipFile);
		}
	}
	
	protected final String renderText(File file, ParseContext context)
			throws ParseException {
		ZipFile zipFile = null;
		Reader reader = null;
		try {
			zipFile = new ZipFile(file);
			ZipEntry contentZipEntry = zipFile.getEntry("content.xml"); //$NON-NLS-1$
			if (contentZipEntry == null)
				throw new ParseException(Msg.file_corrupted.get());
			
			InputStream in = zipFile.getInputStream(contentZipEntry);
			in = new OpenDocumentTextInputStream(in);
			reader = new BufferedReader(new InputStreamReader(in, "utf8"));
			
			return CharStreams.toString(reader);
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
		finally {
			Closeables.closeQuietly(reader);
			UtilParser.closeZipFile(zipFile);
		}
	}
	
	/**
	 * Returns the textual content inside the given HTML element from the given
	 * HTML source. Returns null if the HTML element is not found.
	 */
	@Nullable
	private static String getElementContent(@NotNull Source source,
											@NotNull String elementName) {
		Element el = source.getNextElement(0, elementName);
		return el == null ? null : CharacterReference.decode(el.getContent());
	}

}
