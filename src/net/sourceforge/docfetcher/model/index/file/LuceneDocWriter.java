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

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.model.Field;
import net.sourceforge.docfetcher.model.parse.ParseResult;

import org.apache.lucene.document.Document;

/**
 * @author Tran Nam Quang
 */
abstract class LuceneDocWriter {
	
	public final void add(	@NotNull FileDocument doc,
							@NotNull File file,
							@NotNull ParseResult parseResult)
			throws IOException {
		Document luceneDoc = createLuceneDoc(doc, file, parseResult);
		write(doc, luceneDoc, true);
	}
	
	public void update(	@NotNull FileDocument doc,
						@NotNull File file,
						@NotNull ParseResult parseResult)
			throws IOException {
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
			luceneDoc.add(Field.UID.create(doc.getUniqueId()));
			luceneDoc.add(Field.FILENAME.create(filename));
			luceneDoc.add(Field.TYPE.create(extension));
			luceneDoc.add(Field.PARSER.create(parseResult.getParserName()));
			String title = parseResult.getTitle();
			if (title != null)
				luceneDoc.add(Field.TITLE.create(title));
			luceneDoc.add(Field.SIZE.create(file.length()));
			luceneDoc.add(Field.LAST_MODIFIED.create(String.valueOf(doc.getLastModified())));
			List<String> authors = parseResult.getAuthors();
			if (authors != null)
				for (String author : authors)
					luceneDoc.add(Field.AUTHOR.create(author));
		}
		
		// Create content field with metadata appended to it
		luceneDoc.add(Field.createContent(parseResult.getContent()));
		StringBuilder metadata = parseResult.getMetadata();
		metadata.append(filename);
		luceneDoc.add(Field.createContent(metadata));
		return luceneDoc;
	}
	
	protected abstract boolean appendMetadata();
	
	public abstract void write(	@NotNull FileDocument doc,
								@NotNull Document luceneDoc,
								boolean added) throws IOException;
	
	public abstract void delete(@NotNull String uid) throws IOException;

}
