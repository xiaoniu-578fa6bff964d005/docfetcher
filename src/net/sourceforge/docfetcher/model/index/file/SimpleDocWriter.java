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

import java.io.Closeable;
import java.io.IOException;

import net.sourceforge.docfetcher.model.index.IndexWriterAdapter;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;

/**
 * @author Tran Nam Quang
 */
final class SimpleDocWriter extends LuceneDocWriter implements Closeable {
	
	private final IndexWriterAdapter writer;
	
	public SimpleDocWriter(@NotNull Directory luceneDir) throws IOException {
		writer = new IndexWriterAdapter(luceneDir);
	}
	
	protected boolean appendMetadata() {
		return true;
	}
	
	public void write(	@NotNull FileDocument doc,
						@NotNull Document luceneDoc,
						@NotNull boolean added) throws IOException,
			CheckedOutOfMemoryError {
		if (added)
			writer.add(luceneDoc);
		else
			writer.update(doc.getUniqueId(), luceneDoc);
	}

	public void delete(String uid) throws IOException {
		writer.delete(uid);
	}

	public final void close() throws IOException {
		writer.close();
	}

}
