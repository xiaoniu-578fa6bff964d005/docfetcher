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

import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class PhraseDetectingQueryParser extends QueryParser {
	
	/*
	 * This class is used for determining whether the parsed query is supported
	 * by the fast-vector highlighter. The latter only supports queries that are
	 * a combination of TermQuery, PhraseQuery and/or BooleanQuery.
	 */
	
	private boolean isPhraseQuery = true;
	
	public PhraseDetectingQueryParser(  String defaultField,
                                        Analyzer analyzer) {
		super( defaultField, analyzer);
	}
	
	public boolean isPhraseQuery() {
		return isPhraseQuery;
	}

	protected Query newFuzzyQuery(	Term term,
									float minimumSimilarity,
									int prefixLength) {
		isPhraseQuery = false;
		return super.newFuzzyQuery(term, minimumSimilarity, prefixLength);
	}

	protected Query newMatchAllDocsQuery() {
		isPhraseQuery = false;
		return super.newMatchAllDocsQuery();
	}

	protected MultiPhraseQuery newMultiPhraseQuery() {
		isPhraseQuery = false;
		return super.newMultiPhraseQuery();
	}

	protected Query newPrefixQuery(Term prefix) {
		isPhraseQuery = false;
		return super.newPrefixQuery(prefix);
	}

	protected Query newWildcardQuery(org.apache.lucene.index.Term t) {
		isPhraseQuery = false;
		return super.newWildcardQuery(t);
	}
	
}