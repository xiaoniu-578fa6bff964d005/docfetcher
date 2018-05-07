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

import java.util.LinkedList;

import net.sourceforge.docfetcher.model.FieldTypes;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.index.IndexWriterAdapter;

import org.ansj.library.DicLibrary;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
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
		Analyzer analyzer = IndexRegistry.getAnalyzer();
		IndexWriterAdapter writer = new IndexWriterAdapter(directory);
		Document doc = new Document();
		doc.add(new Field("content", "some text", FieldTypes.TYPE_TEXT_WITH_POSITIONS_OFFSETS_NOT_STORED));
		writer.add(doc);
		Closeables.closeQuietly(writer); // flush unwritten documents into index
		
		// Perform phrase search
		QueryParser queryParser = new QueryParser("content", analyzer);
		Query query = queryParser.parse("\"text\"");
		FastVectorHighlighter highlighter = new FastVectorHighlighter(true, true, null, null);
		FieldQuery fieldQuery = highlighter.getFieldQuery(query);
		IndexSearcher searcher = null;
		try {

			searcher = new IndexSearcher(DirectoryReader.open(directory));
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
			Closeables.closeQuietly(searcher.getIndexReader());
		}
	}

	@Test
	public void testChinesePhraseHighlighter() throws Exception {
		// Create index
		Directory directory = new RAMDirectory();
		//Analyzer analyzer = new StandardAnalyzer( CharArraySet.EMPTY_SET );
		Analyzer analyzer = IndexRegistry.getAnalyzer();
		IndexWriterAdapter writer = new IndexWriterAdapter(directory);
		Document doc = new Document();
		DicLibrary.insert(DicLibrary.DEFAULT, "交通安全", "ansj", 2000);
		DicLibrary.insert(DicLibrary.DEFAULT, "交通", "ansj", 2000);
		DicLibrary.insert(DicLibrary.DEFAULT, "安全", "ansj", 2000);
		String content="注意交通安全出行：不强行上下车，做到先下后上，候车要排队，按秩序上车；下车后要等车辆开走后再行走，如要穿越马路，一定要确保安全的情况下穿行；交通信号灯的正确使用，什么事交通安全出行交通信号灯的正确使用，什么事交通安全出行";
		doc.add(new Field("content", content, FieldTypes.TYPE_TEXT_WITH_POSITIONS_OFFSETS_NOT_STORED));
		writer.add(doc);
		Closeables.closeQuietly(writer); // flush unwritten documents into index

		// Perform phrase search
		QueryParser queryParser = new QueryParser("content", analyzer);
		Query query = queryParser.parse("content:\"交通安全出行\"");
		FastVectorHighlighter highlighter = new FastVectorHighlighter(true, true, null, null);
		FieldQuery fieldQuery = highlighter.getFieldQuery(query);
		IndexSearcher searcher = null;
		try {

			searcher = new IndexSearcher(DirectoryReader.open(directory));
			TopDocs docs = searcher.search(query, 10);
			assertEquals(1, docs.scoreDocs.length);
			int docId = docs.scoreDocs[0].doc;

			// Get phrase highlighting offsets
			FieldTermStack fieldTermStack = new FieldTermStack(searcher.getIndexReader(), docId, "content", fieldQuery);
			FieldPhraseList fieldPhraseList = new FieldPhraseList( fieldTermStack, fieldQuery );
			java.lang.reflect.Field field = fieldPhraseList.getClass().getDeclaredField("phraseList");
			field.setAccessible(true);
			LinkedList<WeightedPhraseInfo> list = (LinkedList<WeightedPhraseInfo>) field.get(fieldPhraseList);
			assertEquals(2, list.get(0).getStartOffset());
			assertEquals(8, list.get(0).getEndOffset());
		} finally {
			Closeables.closeQuietly(searcher.getIndexReader());
		}
	}
}
