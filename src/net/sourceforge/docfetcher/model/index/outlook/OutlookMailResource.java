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

package net.sourceforge.docfetcher.model.index.outlook;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sourceforge.docfetcher.model.FileResource;
import net.sourceforge.docfetcher.model.HotColdFileCache;
import net.sourceforge.docfetcher.model.MailResource;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.model.search.HighlightService;
import net.sourceforge.docfetcher.model.search.HighlightedString;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.apache.lucene.search.Query;

import com.pff.PSTMessage;

/**
 * @author Tran Nam Quang
 */
public final class OutlookMailResource extends MailResource {
	
	private final String subject;
	private final HighlightedString body;
	private final String sender;
	private final List<String> recipients;
	private final Date date;
	private final List<Attachment> attachments;
	
	OutlookMailResource(@NotNull IndexingConfig config,
						@NotNull Query query,
						boolean isPhraseQuery,
						@NotNull final HotColdFileCache unpackCache,
						@NotNull final Path emailId,
						@NotNull PSTMessage email) throws ParseException,
			CheckedOutOfMemoryError {
		Util.checkNotNull(config, unpackCache, email);
		
		subject = email.getSubject();
		body = HighlightService.highlight(query, isPhraseQuery, email.getBody());
		sender = OutlookContext.getSender(email);
		recipients = OutlookContext.getRecipients(email);
		date = email.getMessageDeliveryTime();
		attachments = new ArrayList<Attachment> (email.getNumberOfAttachments());
		
		new AttachmentVisitor(config, email, false) {
			// TODO now: tell attachment visitor to skip a file if it was found in the cache
			protected void handleAttachment(String filename, File tempFile)
					throws ParseException {
				Path cacheKey = emailId.createSubPath(filename);
				FileResource fileResource = unpackCache.putIfAbsent(cacheKey, tempFile);
				attachments.add(new Attachment(filename, fileResource));
			}
			protected void handleException(	String filename,
											Throwable e) {
				// TODO now: if parsing of this file failed, show an error for the
				// file on the preview panel
			}
		}.run();
	}

	@NotNull
	public String getSubject() {
		return subject;
	}

	@NotNull
	public HighlightedString getBody() {
		return body;
	}

	@NotNull
	public String getSender() {
		return sender;
	}
	
	@NotNull
	public List<String> getRecipients() {
		return Collections.unmodifiableList(recipients);
	}

	@NotNull
	public Date getDate() {
		return date;
	}

	@Immutable
	@NotNull
	public List<Attachment> getAttachments() {
		return Collections.unmodifiableList(attachments);
	}
	
}
