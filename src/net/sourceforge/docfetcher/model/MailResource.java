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

package net.sourceforge.docfetcher.model;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sourceforge.docfetcher.model.search.HighlightedString;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
public abstract class MailResource {
	
	public final class Attachment {
		private final String filename;
		private final FileResource fileResource;

		public Attachment(	@NotNull String filename,
							@NotNull FileResource fileResource) {
			this.filename = Util.checkNotNull(filename);
			this.fileResource = Util.checkNotNull(fileResource);
		}
		@NotNull
		public String getFilename() {
			return filename;
		}
		@NotNull
		public File getFile() {
			return fileResource.getFile();
		}
		@Nullable
		public HighlightedString getHighlightedText() {
			// TODO implement this
			// TODO what to do if attachment cannot be parsed? (e.g. archive) -> return null?
			return null;
		}
	}

	public final void dispose() {
		for (Attachment attachment : getAttachments())
			attachment.fileResource.dispose();
	}

	@NotNull
	public abstract String getSubject();

	@NotNull
	public abstract HighlightedString getBody();

	@NotNull
	public abstract String getSender();
	
	@NotNull
	public abstract List<String> getRecipients();

	@NotNull
	public abstract Date getDate();

	@Immutable
	@NotNull
	public abstract List<Attachment> getAttachments();

}