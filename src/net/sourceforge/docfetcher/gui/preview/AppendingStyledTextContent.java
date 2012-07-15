/*******************************************************************************
 * Copyright (c) 2012 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.gui.preview;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Maps;

/**
 * An implementation of StyledTextContent that is optimized for appending text
 * at the end. This class does not support inserting text anywhere else.
 * 
 * @author Tran Nam Quang
 */
final class AppendingStyledTextContent implements StyledTextContent {
	
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		StyledText st = new StyledText(shell, SWT.BORDER | SWT.WRAP);
		st.setContent(new AppendingStyledTextContent());
		
		int n = 10;
		for (int i = 0; i < n; i++)
			st.append("line " + i + (i < n - 1 ? "\n" : ""));
		
		Util.setCenteredBounds(shell, 400, 300);
		shell.open();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
		display.dispose();
	}
	
	private abstract class LineBreaker {
		public LineBreaker(@NotNull String text) {
			final int len = text.length();
			boolean afterR = false;
			for (int i = 0; i < len; i++) {
				char c = text.charAt(i);
				if (afterR) {
					if (c == '\r') {
						handleBreak(i - 1, i);
					} else if (c == '\n') {
						handleBreak(i - 1, i + 1);
						afterR = false;
					} else {
						handleBreak(i - 1, i);
						afterR = false;
					}
				} else {
					if (c == '\r') {
						afterR = true;
					} else if (c == '\n') {
						handleBreak(i, i + 1);
					}
				}
			}
			if (afterR)
				handleBreak(len - 1, len);
		}
		protected abstract void handleBreak(int start, int end);
	}
	
	private final StringBuilder buffer = new StringBuilder();
	private final Map<Integer, Integer> lineToOffset = Maps.newHashMap();
	private final List<TextChangeListener> listeners = new LinkedList<TextChangeListener>();
	
	public AppendingStyledTextContent() {
		lineToOffset.put(0, 0);
	}
	
	public void addTextChangeListener(TextChangeListener listener) {
		listeners.add(listener);
	}
	
	public void removeTextChangeListener(TextChangeListener listener) {
		listeners.remove(listener);
	}

	public int getCharCount() {
		return buffer.length();
	}
	
	public int getLineCount() {
		return lineToOffset.size();
	}

	public String getLineDelimiter() {
		return Util.LS;
	}
	
	public String getLine(int lineIndex) {
		Integer start = lineToOffset.get(lineIndex);
		Integer end = lineToOffset.get(lineIndex + 1);
		if (end == null)
			end = buffer.length();
		// This code has to be really fast
		int lineLength = end - start;
		if (lineLength >= 1) {
			char c1 = buffer.charAt(end - 1);
			if (c1 == '\n') {
				if (lineLength >= 2) {
					char c2 = buffer.charAt(end - 2);
					if (c2 == '\r')
						return buffer.substring(start, end - 2);
				}
				return buffer.substring(start, end - 1);
			}
			else if (c1 == '\r') {
				return buffer.substring(start, end - 1);
			}
		}
		return buffer.substring(start, end);
	}

	public int getLineAtOffset(int offset) {
		if (offset == buffer.length())
			return lineToOffset.size() - 1;
		// Binary search
		int low = 0;
		int high = lineToOffset.size() - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			Integer startOffset = lineToOffset.get(mid);
			if (offset < startOffset) {
				high = mid - 1;
			}
			else {
				Integer endOffset = lineToOffset.get(mid + 1);
				if (endOffset == null)
					endOffset = buffer.length();
				if (offset >= endOffset)
					low = mid + 1;
				else
					return mid;
			}
		}
		throw new IllegalArgumentException();
	}

	public int getOffsetAtLine(int lineIndex) {
		return lineToOffset.get(lineIndex);
	}
	
	public String getTextRange(int start, int length) {
		return buffer.substring(start, start + length);
	}

	public void replaceTextRange(int start, int replaceLength, String text) {
		// Only append operation is supported
		Util.checkThat(start == buffer.length() && replaceLength == 0);
		if (text.isEmpty())
			return;

		final int shift = buffer.length();
		final Map<Integer, Integer> map = Maps.newHashMap();
		
		new LineBreaker(text) {
			protected void handleBreak(int start, int end) {
				int lineStart = end + shift;
				int lineIndex = lineToOffset.size() + map.size();
				map.put(lineIndex, lineStart);
			}
		};
		
		TextChangingEvent event1 = new TextChangingEvent(this);
		event1.start = start;
		event1.newText = text;
		event1.replaceCharCount = replaceLength;
		event1.newCharCount = text.length();
		event1.replaceLineCount = 0;
		event1.newLineCount = map.size();
		for (TextChangeListener listener : listeners)
			listener.textChanging(event1);
		
		buffer.append(text);
		lineToOffset.putAll(map);
		
		TextChangedEvent event2 = new TextChangedEvent(this);
		for (TextChangeListener listener : listeners)
			listener.textChanged(event2);
	}

	public void setText(String text) {
		buffer.delete(0, buffer.length());
		lineToOffset.clear();
		lineToOffset.put(0, 0);
		
		buffer.append(text);
		new LineBreaker(text) {
			protected void handleBreak(int start, int end) {
				int lineStart = end;
				int lineIndex = lineToOffset.size();
				lineToOffset.put(lineIndex, lineStart);
			}
		};
		
		TextChangedEvent event = new TextChangedEvent(this);
		for (TextChangeListener listener : listeners)
			listener.textSet(event);
	}
	
}
