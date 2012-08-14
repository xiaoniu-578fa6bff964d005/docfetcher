/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sourceforge.docfetcher.model.Fields;
import net.sourceforge.docfetcher.model.parse.ParseResult;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.apache.lucene.document.Document;

/**
 * @author Tran Nam Quang
 */
abstract class LuceneDocWriter {
	
	public final void add(	@NotNull FileDocument doc,
							@NotNull File file,
							@NotNull ParseResult parseResult)
			throws IOException, CheckedOutOfMemoryError {
		Document luceneDoc = createLuceneDoc(doc, file, parseResult);
		write(doc, luceneDoc, true);
	}
	
	public void update(	@NotNull FileDocument doc,
						@NotNull File file,
						@NotNull ParseResult parseResult)
			throws IOException, CheckedOutOfMemoryError {
		Document luceneDoc = createLuceneDoc(doc, file, parseResult);
		write(doc, luceneDoc, false);
	}

	@NotNull
	private Document createLuceneDoc(	@NotNull FileDocument doc,
										@NotNull File file,
										@NotNull ParseResult parseResult) {
		/*
		 * The given file might be a temporary one, so we'll have to get the
		 * original filename and last-modified value from the document.
		 */
		Document luceneDoc = new Document();
		String filename = doc.getName();
		
		if (appendMetadata()) {
			String extension = Util.getExtension(filename);
			luceneDoc.add(Fields.UID.create(doc.getUniqueId()));
			luceneDoc.add(Fields.FILENAME.create(filename));
			luceneDoc.add(Fields.TYPE.create(extension));
			luceneDoc.add(Fields.PARSER.create(parseResult.getParserName()));
			String title = parseResult.getTitle();
			if (title == null || title.trim().isEmpty())
				luceneDoc.add(Fields.TITLE.create(Util.splitFilename(filename)[0]));
			else
				luceneDoc.add(Fields.TITLE.create(title));
			luceneDoc.add(Fields.SIZE.create(file.length()));
			luceneDoc.add(Fields.LAST_MODIFIED.create(String.valueOf(doc.getLastModified())));
			List<String> authors = parseResult.getAuthors();
			if (authors != null)
				for (String author : authors)
					luceneDoc.add(Fields.AUTHOR.create(author));
		}
		
		/*
		 * Create content field with metadata appended to it. Note that two
		 * versions of the filename are appended: The filename with and without
		 * file extension. The reason for this is that Lucene's StandardAnalyzer
		 * won't split the filename at the dot before the file extension, so the
		 * user wouldn't find the file if we store only the full filename and
		 * the user searches for the filename without extension.
		 */
		luceneDoc.add(Fields.createContent(parseResult.getContent()));
		StringBuilder metadata = parseResult.getMetadata();
		metadata.append(filename);
		String basename = Util.splitFilename(filename)[0];
		if (!basename.equals(filename)) {
			metadata.append(" ");
			metadata.append(basename);
		}
		luceneDoc.add(Fields.createContent(metadata));
		return luceneDoc;
	}
	
	protected abstract boolean appendMetadata();
	
	public abstract void write(	@NotNull FileDocument doc,
								@NotNull Document luceneDoc,
								boolean added) throws IOException,
			CheckedOutOfMemoryError;
	
	public abstract void delete(@NotNull String uid) throws IOException;

}
