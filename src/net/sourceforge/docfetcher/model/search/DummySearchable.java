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

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.Weight;

/**
 * @author Tran Nam Quang
 */
final class DummySearchable implements Searchable {

	public void search(Weight weight, Filter filter, Collector collector)
			throws IOException {
	}

	public void close() throws IOException {
	}

	public int docFreq(Term term) throws IOException {
		return 0;
	}

	public int[] docFreqs(Term[] terms) throws IOException {
		return new int[terms.length];
	}

	public int maxDoc() throws IOException {
		return 0;
	}

	public TopDocs search(Weight weight, Filter filter, int n)
			throws IOException {
		return new TopDocs(0, new ScoreDoc[0], Float.NaN);
	}

	public Document doc(int i) throws CorruptIndexException, IOException {
		throw new UnsupportedOperationException();
	}

	public Document doc(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException {
		throw new UnsupportedOperationException();
	}

	public Query rewrite(Query query) throws IOException {
		return query;
	}

	public Explanation explain(Weight weight, int doc) throws IOException {
		throw new UnsupportedOperationException();
	}

	public TopFieldDocs search(Weight weight, Filter filter, int n, Sort sort)
			throws IOException {
		return new TopFieldDocs(0, new ScoreDoc[0], new SortField[0], Float.NaN);
	}

}
