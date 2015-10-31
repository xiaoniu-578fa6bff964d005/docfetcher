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

package net.sourceforge.docfetcher.model.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import net.sourceforge.docfetcher.model.DocumentType;
import net.sourceforge.docfetcher.model.Fields;
import net.sourceforge.docfetcher.model.FileResource;
import net.sourceforge.docfetcher.model.MailResource;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.file.FileFactory;
import net.sourceforge.docfetcher.model.index.outlook.OutlookMailFactory;
import net.sourceforge.docfetcher.model.parse.ChmParser;
import net.sourceforge.docfetcher.model.parse.HtmlParser;
import net.sourceforge.docfetcher.model.parse.PageHandler;
import net.sourceforge.docfetcher.model.parse.PagingChmParser;
import net.sourceforge.docfetcher.model.parse.PagingPdfParser;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.model.parse.Parser;
import net.sourceforge.docfetcher.model.parse.PdfParser;
import net.sourceforge.docfetcher.model.parse.TextParser;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

/**
 * @author Tran Nam Quang
 * 
 * thread-safe class
 * throws UnsupportedOperationException if file methods are called on an email
 * object and vice versa.
 */
@ThreadSafe
public final class ResultDocument {
	
	public interface PreviewPageHandler {
		public void handlePage(HighlightedString pageText);
		public boolean isStopped();
	}
	
	private final Document luceneDoc;
	private final float score;
	private final Query query;
	private final boolean isPhraseQuery;
	private final IndexingConfig config;
	private final FileFactory fileFactory;
	private final OutlookMailFactory mailFactory;
	
	// Cached values
	private final String uid;
	private final boolean isEmail;
	private Path path;
	private Path parentPath;
	private long sizeInKB = -1;
	private String parserName;
	
	public ResultDocument(	@NotNull Document luceneDoc,
							float score,
							@NotNull Query query,
							boolean isPhraseQuery,
							@NotNull IndexingConfig config,
							@NotNull FileFactory fileFactory,
							@NotNull OutlookMailFactory mailFactory) {
		Util.checkNotNull(luceneDoc, query, config, fileFactory, mailFactory);
		this.luceneDoc = luceneDoc;
		this.score = score;
		this.query = query;
		this.isPhraseQuery = isPhraseQuery;
		this.config = config;
		this.fileFactory = fileFactory;
		this.mailFactory = mailFactory;
		
		uid = luceneDoc.get(Fields.UID.key());
		isEmail = DocumentType.isEmailType(uid);
	}
	
	private void onlyFiles() {
		if (isEmail)
			throw new UnsupportedOperationException();
	}
	
	private void onlyEmails() {
		if (! isEmail)
			throw new UnsupportedOperationException();
	}
	
	// returns filename title or email subject
	@NotNull
	public String getTitle() {
		String title = luceneDoc.get(Fields.TITLE.key());
		if (title == null)
			title = luceneDoc.get(Fields.SUBJECT.key());
		if (title != null && !title.trim().isEmpty())
			return title;
		if (isEmail) // Bug #3536283: Email subject may be empty
			return "";
		return Util.splitFilename(getFilename())[0];
	}
	
	// score from 0 to 100
	public int getScore() {
		return Math.round(score * 100);
	}
	
	public long getSizeInKB() {
		if (sizeInKB < 0) {
			String sizeString = luceneDoc.get(Fields.SIZE.key());
			assert sizeString != null;
			long sizeInBytes = Long.valueOf(sizeString);
			long extra = sizeInBytes % 1024 == 0 ? 0 : 1;
			sizeInKB = sizeInBytes / 1024 + extra;
		}
		return sizeInKB;
	}
	
	// Returns Field.EMAIL_PARSER for emails
	@NotNull
	public String getParserName() {
		if (parserName == null)
			parserName = luceneDoc.get(Fields.PARSER.key());
		assert parserName != null;
		return parserName;
	}
	
	@NotNull
	public String getFilename() {
		onlyFiles();
		return luceneDoc.get(Fields.FILENAME.key());
	}
	
	@NotNull
	public String getSender() {
		onlyEmails();
		return luceneDoc.get(Fields.SENDER.key());
	}
	
	// returns file extension or mail type (Outlook, IMAP, etc.)
	@NotNull
	public String getType() {
		String type = luceneDoc.get(Fields.TYPE.key());
		assert type != null;
		return type;
	}
	
	@NotNull
	public Path getPath() {
		if (path == null)
			path =  DocumentType.extractPath(uid);
		return path;
	}
	
	@NotNull
	public Path getParentPath() {
		if (parentPath == null)
			parentPath = getPath().splitAtLastSeparator().getLeft();
		return parentPath;
	}
	
	// Returns authors for files, sender for emails
	@NotNull
	public String getAuthors() {
		String[] authors = luceneDoc.getValues(Fields.AUTHOR.key());
		if (authors.length > 0)
			return Util.join(", ", (Object[]) authors);
		String sender = luceneDoc.get(Fields.SENDER.key());
		return sender == null ? "" : sender;
	}
	
	@NotNull
	public Date getLastModified() {
		onlyFiles();
		String lastModified = luceneDoc.get(Fields.LAST_MODIFIED.key());
		return new Date(Long.valueOf(lastModified));
	}
	
	@Nullable
	public Date getDate() {
		onlyEmails();
		String sendDate = luceneDoc.get(Fields.DATE.key());
		return sendDate == null ? null : new Date(Long.valueOf(sendDate));
	}
	
	public boolean isEmail() {
		return isEmail;
	}

	public boolean isHtmlFile() {
		return wasParsedBy(HtmlParser.class);
	}
	
	public boolean isPdfFile() {
		return wasParsedBy(PdfParser.class);
	}
	
	public boolean isChmFile() {
		return wasParsedBy(ChmParser.class);
	}
	
	public boolean isPlainTextFile() {
		return wasParsedBy(TextParser.class);
	}
	
	private boolean wasParsedBy(Class<? extends Parser> parserClass) {
		String parserName = luceneDoc.get(Fields.PARSER.key());
		return parserName.equals(parserClass.getSimpleName());
	}
	
	// Should be run in a thread
	// thrown parse exception has localized error message
	@NotNull
	private String getText() throws ParseException, FileNotFoundException,
			CheckedOutOfMemoryError {
		onlyFiles();
		String parserName = luceneDoc.get(Fields.PARSER.key());
		FileResource fileResource = null;
		try {
			fileResource = getFileResource();
			File file = fileResource.getFile();
			return ParseService.renderText(
				config, file, getFilename(), parserName);
		}
		finally {
			if (fileResource != null)
				fileResource.dispose();
		}
	}
	
	// Should be run in a thread
	// thrown parse exception has localized error message
	@NotNull
	public HighlightedString getHighlightedText() throws ParseException,
			FileNotFoundException, CheckedOutOfMemoryError {
		return HighlightService.highlight(query, isPhraseQuery, getText());
	}
	
	// should be run in a thread
	public void readPages(@NotNull final PreviewPageHandler pageHandler)
			throws ParseException, FileNotFoundException,
			CheckedOutOfMemoryError {
		// TODO i18n of error messages
		onlyFiles();
		Util.checkNotNull(pageHandler);
		FileResource fileResource = null;
		try {
			fileResource = getFileResource();
			PageHandler handler = new PageHandler() {
				public boolean handlePage(String pageText) {
					HighlightedString string;
					try {
						string = HighlightService.highlight(
								query, isPhraseQuery, pageText
						);
					}
					catch (CheckedOutOfMemoryError e) {
						throw new OutOfMemoryError(e.getMessage());
					}
					pageHandler.handlePage(string);
					return pageHandler.isStopped();
				}
			};
			if (isPdfFile()) {
				new PagingPdfParser(fileResource.getFile(), handler).run();
			} else if (isChmFile()) {
				new PagingChmParser(fileResource.getFile(), handler).run();
			}
		}
		catch (OutOfMemoryError e) {
			throw new CheckedOutOfMemoryError(e);
		}
		finally {
			if (fileResource != null)
				fileResource.dispose();
		}
	}

	/**
	 * If the receiver represents a file, this method returns a {@code File} for
	 * it, wrapped in a {@code FileResource}. The caller <b>must</b> dispose of
	 * the FileResource after usage. The disposal is necessary since the
	 * returned {@code File} may represent a temporary file that was extracted
	 * from an archive and therefore needs to be disposed of after usage.
	 * <p>
	 * This operation may take a long time, so it should be run in a non-GUI
	 * thread.
	 */
	@NotNull
	public FileResource getFileResource() throws ParseException,
			FileNotFoundException {
		onlyFiles();
		return fileFactory.createFile(config, getPath());
	}

	/**
	 * If the receiver represents an email, this method loads the email contents
	 * from disk and returns it as a {@code MailResource}. The caller
	 * <b>must</b> dispose of the MailResource after usage.
	 * <p>
	 * This operation may take a long time, so it should be run in a non-GUI
	 * thread.
	 */
	@NotNull
	public MailResource getMailResource() throws ParseException,
			FileNotFoundException, CheckedOutOfMemoryError {
		onlyEmails();
		return mailFactory.createMail(config, query, isPhraseQuery, getPath());
	}

}
