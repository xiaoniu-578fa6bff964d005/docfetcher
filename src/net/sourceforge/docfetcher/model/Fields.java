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
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LegacyLongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.awt.*;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public enum Fields {

	// TODO pre-release: check correctness of usage of Store.XXX, Index.XXX options
	// TODO pre-release: check if storing the file extension is necessary
	// TODO pre-release: some enums here are not used yet
	
	/*
	 * Note: All information that will be displayed on the result table must be
	 * stored in the Lucene index (via Store.YES), because the user might have
	 * indexed a document repository on a removable media, which may or may not
	 * be available during searches. On the other hand, the information that is
	 * displayed on the preview panel should be loaded from the document
	 * repository rather than the Lucene index in order to display up-to-date
	 * content.
	 */
	
	// Fields available for files and emails
	UID (StringField.TYPE_STORED), // Index.NO will cause deletions to fail
	CONTENT (TextField.TYPE_NOT_STORED),
	CONTENT_WITH_OFFSET (FieldTypes.TYPE_TEXT_WITH_POSITIONS_OFFSETS_STORED),
	TYPE (StringField.TYPE_STORED), // file extension or email type (outlook, imap, etc.)
	// The following must be stored as a numeric field in order to enable
	// filtering and sorting for the web interface
	SIZE (LegacyLongField.TYPE_STORED),
	PARSER (StringField.TYPE_STORED), // Use constant EMAIL_PARSER for emails

	// Fields available for files
	FILENAME (TextField.TYPE_STORED),
	TITLE (TextField.TYPE_STORED),
	AUTHOR (TextField.TYPE_STORED),
	LAST_MODIFIED (StringField.TYPE_STORED),

	// Fields available for emails
	SUBJECT (TextField.TYPE_STORED),
	SENDER (TextField.TYPE_STORED),
	RECIPIENTS (TextField.TYPE_STORED), // TODO post-release-1.1: show this field on results panel in "email mode"
	DATE (StringField.TYPE_STORED), // this field is optional
	;
	public static final String EMAIL_PARSER = "EmailParser";
	
	@NotNull private final String key;
	@NotNull private final FieldType type;

	private Fields(	@NotNull FieldType type) {
		this.key = this.name().toLowerCase();
		this.type=type;
	}

	@NotNull
	public String key() {
		return key;
	}
	
	// For long values, use create(long) instead
	@NotNull
	public Field create(@NotNull String fieldValue) {
		return new Field(key, fieldValue, type);
	}
	
	@NotNull
	public LegacyLongField create(long fieldValue) {
		return new LegacyLongField(key, fieldValue, type);
	}

	@NotNull
	private Field create(CharSequenceReader charSequenceReader) {
		return new Field(key,charSequenceReader,type);
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
		// WITH_POSITIONS_OFFSETS is required by the fast-vector highlighter
		if (fieldValue instanceof String) {
			if(withOffsets)
                return CONTENT_WITH_OFFSET.create((String) fieldValue);
			else
				return CONTENT.create((String) fieldValue);
		}
		if(withOffsets)
			return CONTENT_WITH_OFFSET.create(new CharSequenceReader().setInput(fieldValue));
		else
			return CONTENT.create(new CharSequenceReader().setInput(fieldValue));
	}

}
