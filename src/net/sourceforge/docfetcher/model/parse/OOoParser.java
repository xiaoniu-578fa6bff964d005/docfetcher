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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import com.catcode.odf.OpenDocumentTextInputStream;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
abstract class OOoParser extends FileParser {
	
	public static final class OOoWriterParser extends OOoParser {
		public OOoWriterParser() {
			super("OpenOffice.org Writer", "odt", "ott"); // TODO i18n filetype_odt
		}
	}
	
	public static final class OOoCalcParser extends OOoParser {
		public OOoCalcParser() {
			super("OpenOffice.org Calc", "ods", "ots"); // TODO i18n filetype_ods
		}
	}
	
	public static final class OOoDrawParser extends OOoParser {
		public OOoDrawParser() {
			super("OpenOffice.org Draw", "odg", "otg"); // TODO i18n filetype_odg
		}
	}
	
	public static final class OOoImpressParser extends OOoParser {
		public OOoImpressParser() {
			super("OpenOffice.org Impress", "odp", "otp"); // TODO i18n filetype_odp
		}
	}
	
	private static final Collection<String> types = Collections.singleton(
		MediaType.application("zip"));
	
	private final String typeLabel;
	private final Collection<String> extensions;
	
	private OOoParser(@NotNull String typeLabel, @NotNull String... extensions) {
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
			ZipEntry manifZipEntry = zipFile.getEntry("META-INF/manifest.xml"); //$NON-NLS-1$
			ZipEntry metaZipEntry = zipFile.getEntry("meta.xml"); //$NON-NLS-1$
			ZipEntry contentZipEntry = zipFile.getEntry("content.xml"); //$NON-NLS-1$
			if (manifZipEntry == null || metaZipEntry == null || contentZipEntry == null)
				throw new ParseException("file_corrupted");

			// Find out if file is password protected
			InputStream manifInputStream = zipFile.getInputStream(manifZipEntry);
			Source manifSource = new Source(manifInputStream);
			Closeables.closeQuietly(manifInputStream);
			manifSource.setLogger(null);
			StartTag encryptTag = manifSource.getNextStartTag(0, "manifest:encryption-data"); //$NON-NLS-1$
			if (encryptTag != null)
				throw new ParseException("doc_pw_protected");
			
			// Get tags from meta.xml file
			InputStream metaInputStream = zipFile.getInputStream(metaZipEntry);
			Source metaSource = new Source(metaInputStream);
			Closeables.closeQuietly(metaInputStream);
			metaSource.setLogger(null);
			String title = getElementContent(metaSource, "dc:title"); //$NON-NLS-1$
			String author = getElementContent(metaSource, "dc:creator"); //$NON-NLS-1$
			String description = getElementContent(metaSource, "dc:description"); //$NON-NLS-1$
			String subject = getElementContent(metaSource, "dc:subject"); //$NON-NLS-1$
			String keyword = getElementContent(metaSource, "meta:keyword"); //$NON-NLS-1$

			// Collect content.xml entries
			List<ZipEntry> contentEntries = new ArrayList<ZipEntry>();
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
				String content = contentElement.getContent().getTextExtractor().toString();
				sb.append(content).append(" "); //$NON-NLS-1$
			}
			
			// Create and return parse result
			ParseResult parseResult = new ParseResult(sb);
			parseResult.setTitle(title);
			parseResult.addAuthor(author);
			parseResult.addMiscMetadata(description);
			parseResult.addMiscMetadata(subject);
			parseResult.addMiscMetadata(keyword);
			return parseResult;
		}
		catch (IOException e) {
			throw new ParseException(e);
		}
		finally {
			closeQuietly(zipFile);
		}
	}

	/**
	 * Returns the textual content inside the given HTML element from the given
	 * HTML source. Returns null if the HTML element is not found.
	 */
	@Nullable
	private String getElementContent(	@NotNull Source source,
										@NotNull String elementName) {
		Element el = source.getNextElement(0, elementName);
		return el == null ? null : CharacterReference.decode(el.getContent());
	}
	
	protected final String renderText(File file, ParseContext context)
			throws ParseException {
		ZipFile zipFile = null;
		Reader reader = null;
		try {
			zipFile = new ZipFile(file);
			ZipEntry contentZipEntry = zipFile.getEntry("content.xml"); //$NON-NLS-1$
			if (contentZipEntry == null)
				throw new ParseException("file_corrupted");
			
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
			closeQuietly(zipFile);
		}
	}
	
	private static void closeQuietly(@Nullable ZipFile zipFile) {
		if (zipFile == null)
			return;
		try {
			zipFile.close();
		}
		catch (IOException e) {
			Util.printErr(e);
		}
	}

}
