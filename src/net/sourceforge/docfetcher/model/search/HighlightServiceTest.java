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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedList;

import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.index.IndexWriterAdapter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FieldTermStack;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
public final class HighlightServiceTest {
	
	@SuppressWarnings("unchecked")
	@Test
	public void testPhraseHighlighter() throws Exception {
		// Create index
		Directory directory = new RAMDirectory();
		Analyzer analyzer = new StandardAnalyzer(
				IndexRegistry.LUCENE_VERSION,
				Collections.EMPTY_SET
		);
		IndexWriterAdapter writer = new IndexWriterAdapter(directory);
		Document doc = new Document();
		doc.add(new Field("content", "some text", Store.NO, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
		writer.add(doc);
		Closeables.closeQuietly(writer); // flush unwritten documents into index
		
		// Perform phrase search
		QueryParser queryParser = new QueryParser(IndexRegistry.LUCENE_VERSION, "content", analyzer);
		Query query = queryParser.parse("\"text\"");
		FastVectorHighlighter highlighter = new FastVectorHighlighter(true, true, null, null);
		FieldQuery fieldQuery = highlighter.getFieldQuery(query);
		IndexSearcher searcher = null;
		try {
			searcher = new IndexSearcher(directory);
			TopDocs docs = searcher.search(query, 10);
			assertEquals(1, docs.scoreDocs.length);
			int docId = docs.scoreDocs[0].doc;
			
			// Get phrase highlighting offsets
			FieldTermStack fieldTermStack = new FieldTermStack(searcher.getIndexReader(), docId, "content", fieldQuery);
			FieldPhraseList fieldPhraseList = new FieldPhraseList( fieldTermStack, fieldQuery );
		    java.lang.reflect.Field field = fieldPhraseList.getClass().getDeclaredField("phraseList");
		    field.setAccessible(true);
		    LinkedList<WeightedPhraseInfo> list = (LinkedList<WeightedPhraseInfo>) field.get(fieldPhraseList);
		    assertEquals(5, list.get(0).getStartOffset());
		    assertEquals(9, list.get(0).getEndOffset());
		} finally {
			Closeables.closeQuietly(searcher);
		}
	}

}
