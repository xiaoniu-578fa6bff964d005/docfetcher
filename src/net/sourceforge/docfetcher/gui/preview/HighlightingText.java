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
import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.model.search.HighlightedString;
import net.sourceforge.docfetcher.model.search.Range;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Tran Nam Quang
 */
final class HighlightingText {
	
	private static final int margin = 10;
	
	@NotNull private StyledText textViewer;
	@NotNull private StyleRange highlightStyle;
	@NotNull private Color highlightColor;
	
	private final List<int[]> rangesList = new ArrayList<int[]>();
	private int occCount;
	private Font normalFont;
	private Font monoFont;
	
	public HighlightingText(@NotNull Composite parent) {
		int style = SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.BORDER;
		textViewer = new StyledText(parent, style);
		textViewer.setMargins(margin, margin, margin, margin);
		setHighlightColorAndStyle();
		
		// Update highlight color when preferences entry changes
		SettingsConf.IntArray.PreviewHighlighting.evtChanged.add(new Event.Listener<int[]>() {
			public void update(int[] eventData) {
				Color oldColor = highlightColor;
				setHighlightColorAndStyle();
				updateHighlighting();
				oldColor.dispose();
			}
		});
		
		// Update normal font when preferences entry changes
		UtilGui.getPreviewFontNormal().evtChanged.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				if (normalFont == null)
					return;
				Font oldFont = normalFont;
				normalFont = UtilGui.getPreviewFontNormal().createFont();
				if (textViewer.getFont() == oldFont)
					textViewer.setFont(normalFont);
				oldFont.dispose();
			}
		});
		
		// Update monospace font when preferences entry changes
		UtilGui.getPreviewFontMono().evtChanged.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				if (monoFont == null)
					return;
				Font oldFont = monoFont;
				monoFont = UtilGui.getPreviewFontMono().createFont();
				if (textViewer.getFont() == oldFont)
					textViewer.setFont(monoFont);
				oldFont.dispose();
			}
		});
		
		// Dispose of fonts and highlight color
		textViewer.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Resource[] resources = new Resource[] {
					normalFont, monoFont, highlightColor };
				for (Resource resource : resources)
					if (resource != null)
						resource.dispose();
			}
		});
	}
	
	private void setHighlightColorAndStyle() {
		int[] rgb = SettingsConf.IntArray.PreviewHighlighting.get();
		highlightColor = new Color(textViewer.getDisplay(), rgb[0], rgb[1], rgb[2]);
		highlightStyle = new StyleRange(0, 0, null, highlightColor);
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
	
	public void setUseMonoFont(boolean useMonoFont) {
		if (useMonoFont) {
			if (monoFont == null)
				monoFont = UtilGui.getPreviewFontMono().createFont();
			textViewer.setFont(monoFont);
		}
		else {
			if (normalFont == null)
				normalFont = UtilGui.getPreviewFontNormal().createFont();
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

	/**
	 * Selects and scrolls to the nearest occurrence, if one exists, starting
	 * from the current selection. If <tt>forward</tt> is true, this method goes
	 * to the next occurrence, otherwise to the previous one.
	 * <p>
	 * This method returns the number of the occurrence that was scrolled to.
	 * The number is 1-based and relative to the occurrences in the receiver. If
	 * no occurrence was found, null is returned.
	 */
	@Nullable
	public Integer goTo(boolean forward) {
		Point sel = textViewer.getSelection();
		int searchStart = forward ? sel.y : sel.x;
		return goTo(forward, searchStart);
	}
	
	// argument is one-based
	public void goTo(int occ) {
		int tokenStart = -1;
		int tokenEnd = -1;
		int tokenIndex = 0;
		
		outer: {
			for (int[] ranges : rangesList) {
				for (int i = 0; i < ranges.length - 1; i += 2) {
					tokenIndex++;
					if (tokenIndex == occ) {
						tokenStart = ranges[i];
						tokenEnd = tokenStart + ranges[i + 1];
						break outer;
					}
				}
			}
		}
		
		if (tokenStart == -1)
			return;
		
		textViewer.setSelection(tokenStart, tokenEnd);
		scrollToMiddle((tokenStart + tokenEnd) / 2);
	}
	
	/**
	 * Selects and scrolls to the last occurrence, if one exists.
	 * <p>
	 * This method returns the number of the occurrence that was scrolled to.
	 * The number is 1-based and relative to the occurrences in the receiver. If
	 * no occurrence was found, null is returned.
	 */
	@Nullable
	public Integer goToLast() {
		return goTo(false, textViewer.getCharCount());
	}
	
	@Nullable
	private Integer goTo(boolean forward, int searchStart) {
		int tokenStart = -1;
		int tokenEnd = -1;
		int tokenIndex = 0;
		
		outer: {
			if (forward) {
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
			/*
			 * Note: Some lines may be wrapped, so it's possible that
			 * linePixelNow != lineHeight * lineNumber.
			 */
			int linePixelTop = textViewer.getTopPixel();
			int linePixelNow = textViewer.getLocationAtOffset(caretOffset).y + linePixelTop - margin;
			int linePixelBottom = linePixelTop + textViewer.getClientArea().height;
			int dist = linePixelBottom - linePixelTop;
			int dist13 = dist / 3;
			int dist23 = 2 * dist / 3;
			double lineIndexMiddleTop = linePixelTop + dist / 3;
			double lineIndexMiddleBottom = linePixelBottom - dist / 3;
			if (linePixelNow < lineIndexMiddleTop)
				textViewer.setTopPixel(linePixelNow - dist13);
			else if (linePixelNow > lineIndexMiddleBottom)
				textViewer.setTopPixel(linePixelNow - dist23);
		}
		catch (Exception e) {
			// Ignore invalid caret offset
		}
	}

}
