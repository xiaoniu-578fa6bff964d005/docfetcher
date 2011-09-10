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

import java.util.Collections;
import java.util.List;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.Immutable;
import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class HighlightedString {
	
	private String string;
	private final List<Range> ranges;
	
	HighlightedString(@NotNull String string, @NotNull List<Range> ranges) {
		Util.checkNotNull(string, ranges);
		this.string = string;
		this.ranges = ranges;
	}
	
	@NotNull
	public String getString() {
		return string;
	}
	
	@Immutable
	@NotNull
	public List<Range> getRanges() {
		return Collections.unmodifiableList(ranges);
	}
	
	public int length() {
		return string.length();
	}
	
	public boolean isEmpty() {
		return string.length() == 0;
	}
	
	public int getRangeCount() {
		return ranges.size();
	}
	
	public void add(@NotNull HighlightedString otherString) {
		Util.checkNotNull(otherString);
		int offset = string.length();
		string = string + otherString.string;
		for (Range range : otherString.ranges)
			ranges.add(new Range(range.start + offset, range.length));
	}

}
