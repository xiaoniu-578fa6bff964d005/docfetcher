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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.Immutable;
import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class HighlightedString {
	
	private final List<String> strings = new ArrayList<String>(1);
	private final List<Range> ranges;
	private int length;
	
	HighlightedString(@NotNull String string, @NotNull List<Range> ranges) {
		Util.checkNotNull(string, ranges);
		strings.add(string);
		length = string.length();
		this.ranges = ranges;
	}
	
	@NotNull
	public String getString() {
		StringBuilder sb = new StringBuilder(length);
		for (String string : strings)
			sb.append(string);
		return sb.toString();
	}
	
	@Immutable
	@NotNull
	public List<Range> getRanges() {
		return Collections.unmodifiableList(ranges);
	}
	
	public int length() {
		return length;
	}
	
	public boolean isEmpty() {
		return length == 0;
	}
	
	public int getRangeCount() {
		return ranges.size();
	}
	
	// The receiver is modified, the given highlighted string is not
	public void add(@NotNull HighlightedString otherString) {
		Util.checkNotNull(otherString);
		strings.addAll(otherString.strings);
		
		// We must create new Range objects here, otherwise we'll modify the
		// given highlighted string
		for (Range range : otherString.ranges)
			ranges.add(new Range(range.start + length, range.length));
		
		length += otherString.length;
	}

}
