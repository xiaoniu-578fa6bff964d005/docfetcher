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

package net.sourceforge.docfetcher.model;

import javolution.io.CharSequenceReader;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.NumericField;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public enum Fields {
	
	// TODO check correctness of usage of Store.XXX, Index.XXX options
	// TODO check if storing the file extension is necessary
	// TODO some enums here are not used yet
	
	/*
	 * Note: All information that will be displayed on the result table must be
	 * stored in the Lucene index (via Store.YES), because the user might have
	 * indexed a document repository on a removable media, which may or may not
	 * be available during searches. On the other hand, the information that is
	 * displayed on the preview panel should loaded from the document repository
	 * rather than the Lucene index in order to display up-to-date content.
	 */
	
	// Fields available for files and emails
	UID (Store.YES, Index.NOT_ANALYZED), // Index.NO will cause deletions to fail
	CONTENT (Store.NO, Index.ANALYZED),
	TYPE (Store.YES, Index.NO), // file extension or email type (outlook, imap, etc.)
	// The following must be stored as a numeric field in order to enable
	// filtering and sorting for the web interface
	SIZE (Store.YES, Index.ANALYZED_NO_NORMS),
	PARSER (Store.YES, Index.NO), // Use constant EMAIL_PARSER for emails
	
	// Fields available for files
	FILENAME (Store.YES, Index.ANALYZED),
	TITLE (Store.YES, Index.ANALYZED),
	AUTHOR (Store.YES, Index.ANALYZED),
	LAST_MODIFIED (Store.YES, Index.NO),
	
	// Fields available for emails
	SUBJECT (Store.YES, Index.ANALYZED),
	SENDER (Store.YES, Index.ANALYZED),
	RECIPIENTS (Store.NO, Index.ANALYZED), // TODO store this field and show on results panel in "email mode"
	DATE (Store.YES, Index.NO),
	;
	
	public static final String EMAIL_PARSER = "EmailParser";
	
	@NotNull private final String key;
	@NotNull private final Store store;
	@NotNull private final Index index;
	
	private Fields(	@NotNull Store store,
					@NotNull Index index) {
		this.key = this.name().toLowerCase();
		this.store = store;
		this.index = index;
	}
	
	@NotNull
	public String key() {
		return key;
	}
	
	// For long values, use create(long) instead
	@NotNull
	public Field create(@NotNull String fieldValue) {
		return new Field(key, fieldValue, store, index);
	}
	
	// The field is always indexed
	@NotNull
	public NumericField create(@NotNull long fieldValue) {
		return new NumericField(key, store, true).setLongValue(fieldValue);
	}
	
	// Will create a tokenized and indexed field that is not stored if the given
	// fieldValue is not a String
	// does not store token positions and offsets
	@NotNull
	public static Field createContent(@NotNull CharSequence fieldValue) {
		return createContent(fieldValue, false);
	}
	
	@NotNull
	public static Field createContent(	@NotNull CharSequence fieldValue,
										boolean withOffsets) {
		// TermVector.WITH_POSITIONS_OFFSETS is required by the fast-vector
		// highlighter
		TermVector termVector = withOffsets
			? TermVector.WITH_POSITIONS_OFFSETS
			: TermVector.NO;
		if (fieldValue instanceof String) {
			return new Field(
				CONTENT.key, (String) fieldValue, CONTENT.store, CONTENT.index,
				termVector);
		}
		return new Field(
			CONTENT.key, new CharSequenceReader().setInput(fieldValue),
			termVector);
	}

}
