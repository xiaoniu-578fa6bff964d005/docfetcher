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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SettingsConf.FontDescription;
import net.sourceforge.docfetcher.model.search.HighlightedString;
import net.sourceforge.docfetcher.model.search.Range;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Tran Nam Quang
 */
final class HighlightingText {
	
	@NotNull private StyledText textViewer;
	private final StyleRange highlightStyle = new StyleRange(0, 0, null, Col.YELLOW.get());
	private final List<int[]> rangesList = new ArrayList<int[]>();
	private int occCount;
	private Font normalFont;
	private Font monoFont;
	
	public HighlightingText(@NotNull Composite parent) {
		int style = SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.BORDER;
		textViewer = new StyledText(parent, style);
		int m = 10;
		textViewer.setMargins(m, m, m, m);
		Util.disposeWith(textViewer, normalFont, monoFont);
	}
	
	@NotNull
	public StyledText getControl() {
		return textViewer;
	}
	
	public void clear() {
		textViewer.setText("");
		rangesList.clear();
		occCount = 0;
	}
	
	public int getOccCount() {
		return occCount;
	}
	
	public void setUseMonoFont(boolean useMonoFont) {
		if (useMonoFont) {
			if (monoFont == null) {
				monoFont = Util.IS_WINDOWS
				? FontDescription.PreviewMonoWindows.get()
				: FontDescription.PreviewMonoLinux.get();
			}
			textViewer.setFont(monoFont);
		}
		else {
			if (normalFont == null) {
				normalFont = Util.IS_WINDOWS
				? FontDescription.PreviewWindows.get()
				: FontDescription.PreviewLinux.get();
			}
			textViewer.setFont(normalFont);
		}
	}
	
	public void setText(@NotNull HighlightedString string) {
		rangesList.clear();
		occCount = 0;
		
		textViewer.setText(string.getString());
		if (string.isEmpty())
			return;
		
		int[] rangeArray = getRangeArray(string, 0);
		if (SettingsConf.Bool.HighlightingEnabled.get()) {
			StyleRange[] styles = getStylesArray(string);
			textViewer.setStyleRanges(rangeArray, styles);
		}
		
		rangesList.add(rangeArray);
		occCount = string.getRangeCount();
	}
	
	public void appendText(@NotNull HighlightedString string) {
		if (string.isEmpty())
			return;
		
		int offset = textViewer.getCharCount();
		textViewer.append(string.getString());
		
		int[] rangeArray = getRangeArray(string, offset);
		if (SettingsConf.Bool.HighlightingEnabled.get()) {
			StyleRange[] styles = getStylesArray(string);
			textViewer.setStyleRanges(offset, string.length(), rangeArray, styles);
		}
		
		rangesList.add(rangeArray);
		occCount += string.getRangeCount();
	}
	
	public void updateHighlighting() {
		if (SettingsConf.Bool.HighlightingEnabled.get()) {
			int[] fullRangeArray = new int[2 * occCount];
			int offset = 0;
			for (int[] array : rangesList) {
				System.arraycopy(array, 0, fullRangeArray, offset, array.length);
				offset += array.length;
			}
			StyleRange[] styleArray = new StyleRange[occCount];
			Arrays.fill(styleArray, highlightStyle);
			textViewer.setStyleRanges(fullRangeArray, styleArray);
		}
		else {
			textViewer.setStyleRanges(new StyleRange[0]);
		}
	}
	
	@Nullable
	public Integer goTo(boolean nextNotPrevious) {
		Point sel = textViewer.getSelection();
		int searchStart = nextNotPrevious ? sel.y : sel.x;
		int tokenStart = -1;
		int tokenEnd = -1;
		int tokenIndex = 0;
		
		outer: {
			if (nextNotPrevious) {
				for (int[] ranges : rangesList) {
					for (int i = 0; i < ranges.length - 1; i += 2) {
						tokenIndex++;
						if (ranges[i] >= searchStart) {
							tokenStart = ranges[i];
							tokenEnd = tokenStart + ranges[i + 1];
							break outer;
						}
					}
				}
			}
			else {
				for (int[] ranges : rangesList) {
					for (int i = 0; i < ranges.length - 1; i += 2) {
						if (ranges[i] + ranges[i + 1] <= searchStart) {
							tokenStart = ranges[i];
							tokenEnd = tokenStart + ranges[i + 1];
							tokenIndex++;
						}
						else {
							break outer;
						}
					}
				}
			}
		}
		
		if (tokenStart == -1)
			return null;
		
		textViewer.setSelection(tokenStart, tokenEnd);
		scrollToMiddle((tokenStart + tokenEnd) / 2);
		return tokenIndex;
	}

	@NotNull
	private static int[] getRangeArray(@NotNull HighlightedString string,
								int offset) {
		List<Range> ranges = string.getRanges();
		int[] rangeArray = new int[ranges.size() * 2];
		for (int i = 0; i < ranges.size(); i++) {
			rangeArray[i * 2] = ranges.get(i).start + offset;
			rangeArray[i * 2 + 1] = ranges.get(i).length;
		}
		return rangeArray;
	}
	
	@NotNull
	private StyleRange[] getStylesArray(@NotNull HighlightedString string) {
		StyleRange[] styles = new StyleRange[string.getRangeCount()];
		Arrays.fill(styles, highlightStyle);
		return styles;
	}
	
	/**
	 * Vertically divides the text viewer into three segments of equal height
	 * and scrolls the given caret offset into view so that it is always
	 * displayed in the middle segment (either at the top or at bottom of it or
	 * somewhere in between).
	 */
	private void scrollToMiddle(int caretOffset) {
		try {
			int lineIndexNow = textViewer.getLineAtOffset(caretOffset);
			int lineIndexTop = textViewer.getTopIndex();
			int lineIndexBottom = textViewer.getLineIndex(textViewer.getClientArea().height);
			double dist = lineIndexBottom - lineIndexTop;
			int dist13 = (int) (dist / 3);
			int dist23 = (int) (2 * dist / 3);
			double lineIndexMiddleTop = lineIndexTop + dist / 3;
			double lineIndexMiddleBottom = lineIndexBottom - dist / 3;
			if (lineIndexNow < lineIndexMiddleTop)
				textViewer.setTopIndex(lineIndexNow - dist13);
			else if (lineIndexNow > lineIndexMiddleBottom)
				textViewer.setTopIndex(lineIndexNow - dist23);
		}
		catch (Exception e) {
			// textViewer.getLineAtOffset(..) can throw an IllegalArgumentException
			// See bug #2778204
		}
	}

}
