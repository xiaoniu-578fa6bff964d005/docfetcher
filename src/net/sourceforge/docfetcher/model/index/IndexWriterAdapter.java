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

package net.sourceforge.docfetcher.model.index;

import java.io.Closeable;
import java.io.IOException;

import net.sourceforge.docfetcher.model.Fields;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;

import com.google.common.io.Closeables;
import org.apache.lucene.util.Version;

/**
 * Wrapper for Lucene's IndexWriter that adds some functionality.
 * 
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class IndexWriterAdapter implements Closeable {
	
	public static final Term idTerm = new Term(Fields.UID.key());
	
	@NotNull private IndexWriter writer;

	public IndexWriterAdapter(@NotNull Directory luceneDir) throws IOException {
		IndexWriterConfig config
				= new IndexWriterConfig(IndexRegistry.LUCENE_VERSION,IndexRegistry.getAnalyzer());
		writer = new IndexWriter(luceneDir, config);
	}

	// may throw OutOfMemoryError
	public void add(@NotNull Document document) throws IOException,
			CheckedOutOfMemoryError {
		try {
			writer.addDocument(document);
		}
		catch (OutOfMemoryError e) {
			reopenWriterAndThrow(e);
		}
		catch (IllegalStateException e) {
			if (e.getMessage().contains("OutOfMemoryError")) {
				reopenWriterAndThrow(e);
			} else {
				throw e;
			}
		}
	}

	// may throw OutOfMemoryError
	public void update(@NotNull String uid, @NotNull Document document)
			throws IOException, CheckedOutOfMemoryError {
		try {
			writer.updateDocument(new Term(idTerm.field(), uid), document);
		}
		catch (OutOfMemoryError e) {
			reopenWriterAndThrow(e);
		}
		catch (IllegalStateException e) {
			if (e.getMessage().contains("OutOfMemoryError")) {
				reopenWriterAndThrow(e);
			} else {
				throw e;
			}
		}
	}
	
	private void reopenWriterAndThrow(@NotNull Throwable t)
			throws IOException, CheckedOutOfMemoryError {
		/*
		 * According to the IndexWriter javadoc, we're supposed to immediately
		 * close the IndexWriter if IndexWriter.addDocument(...) or
		 * IndexWriter.updateDocument(...) hit OutOfMemoryErrors.
		 */
		Directory indexDir = writer.getDirectory();
		Closeables.closeQuietly(writer);
		IndexWriterConfig config
				= new IndexWriterConfig(IndexRegistry.LUCENE_VERSION,IndexRegistry.getAnalyzer());
		writer = new IndexWriter(indexDir, config);
		throw new CheckedOutOfMemoryError(t);
	}

	public void delete(@NotNull String uid) throws IOException {
		writer.deleteDocuments(new Term(idTerm.field(),uid));
	}
	
	public void close() throws IOException {
		writer.close();
	}

}
