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

package net.sourceforge.docfetcher.gui.preview;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.gui.Col;
import net.sourceforge.docfetcher.model.search.HighlightedString;
import net.sourceforge.docfetcher.model.search.Range;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Tran Nam Quang
 */
final class HighlightingText {
	
	@NotNull private StyledText textViewer;
	
	public HighlightingText(@NotNull Composite parent) {
		int style = SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL;
		textViewer = new StyledText(parent, style);
	}
	
	@NotNull
	public Control getControl() {
		return textViewer;
	}
	
	public void clear() {
		if (textViewer.isDisposed()) return;
		textViewer.setText("");
	}
	
	public void setText(@NotNull HighlightedString string) {
		if (textViewer.isDisposed()) return;
		textViewer.setText(string.getString());
		if (string.isEmpty()) return;
		int[] rangeArray = getRangeArray(string, 0);
		StyleRange[] styles = getStylesArray(string);
		textViewer.setStyleRanges(rangeArray, styles);
	}
	
	public void appendText(@NotNull HighlightedString string) {
		if (textViewer.isDisposed()) return;
		if (string.isEmpty()) return;
		int offset = textViewer.getCharCount();
		textViewer.append(string.getString());
		int [] rangeArray = getRangeArray(string, offset);
		StyleRange[] styles = getStylesArray(string);
		textViewer.setStyleRanges(offset, string.length(), rangeArray, styles);
	}

	private int[] getRangeArray(HighlightedString string,
								int offset) {
		List<Range> ranges = string.getRanges();
		int[] rangeArray = new int[ranges.size() * 2];
		for (int i = 0; i < ranges.size(); i++) {
			rangeArray[i * 2] = ranges.get(i).start + offset;
			rangeArray[i * 2 + 1] = ranges.get(i).length;
		}
		return rangeArray;
	}
	
	private StyleRange[] getStylesArray(HighlightedString string) {
		StyleRange style = new StyleRange(0, 0, null, Col.YELLOW.get());
		StyleRange[] styles = new StyleRange[string.getRangeCount()];
		Arrays.fill(styles, style);
		return styles;
	}

}
