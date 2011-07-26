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

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;
import java.io.IOException;

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.model.Field;
import net.sourceforge.docfetcher.model.parse.ParseResult;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

/**
 * @author Tran Nam Quang
 */
final class AppendingDocWriter extends LuceneDocWriter {
	
	@Nullable private Document luceneDoc;
	
	protected boolean appendMetadata() {
		// Only append metadata for the first document
		return luceneDoc == null;
	}
	
	public void write(	@NotNull FileDocument doc,
						@NotNull Document luceneDoc,
						boolean added) throws IOException {
		if (this.luceneDoc == null)
			this.luceneDoc = luceneDoc;
		else
			for (Fieldable field : luceneDoc.getFields(Field.CONTENT.key()))
				this.luceneDoc.add(field);
	}

	public void update(	@NotNull FileDocument doc,
						@NotNull File file,
						@NotNull ParseResult parseResult) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void delete(@NotNull String uid) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Nullable
	public Document getLuceneDoc() {
		return luceneDoc;
	}

}
