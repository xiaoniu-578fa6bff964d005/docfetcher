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

package net.sourceforge.docfetcher.base.gui;

import java.util.LinkedList;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.base.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

/**
 * @author Tran Nam Quang
 */
public final class UndoRedoProvider {
	
	private static Pattern leadingSpaces = Pattern.compile("[\\s&&[^\\n\\r]]*\\S+");
	private static Pattern trailingSpaces = Pattern.compile("\\S+[\\s&&[^\\n\\r]]*");
	
	private UndoRedoProvider() {
	}
	
	public static void applyTo(final StyledText st, final int undoSize) {
		Util.checkNotNull(st);
		Util.checkThat(undoSize >= 0);
		
		final LinkedList<Edit> undoStack = new LinkedList<Edit> ();
		final LinkedList<Edit> redoStack = new LinkedList<Edit> ();
		
		final ExtendedModifyListener modifyListener = new ExtendedModifyListener() {
			public void modifyText(ExtendedModifyEvent event) {
				undoStack.add(new Edit(event.start, event.length, event.replacedText));
				mergeLastUndos();
				while(undoStack.size() > undoSize)
					undoStack.removeFirst();
				redoStack.clear();
			}
			private void mergeLastUndos() {
				if (undoStack.size() <= 1) return;
				Edit e1 = undoStack.get(undoStack.size() - 2);
				Edit e2 = undoStack.getLast();
				
				// Case 1: A continuous sequence of characters was inserted
				if (e1.start + e1.length == e2.start
						&& e1.text.equals("") && e2.text.equals("")) {
					String text = st.getTextRange(e1.start, e1.length + e2.length);
					if (trailingSpaces.matcher(text).matches()) {
						int length = e1.length + e2.length;
						doMergeLastUndos(new Edit(e1.start, length, ""));
					}
				}
				else if (e1.length == 0 && e2.length == 0) {
					// Case 2: A continuous sequence of characters was deleted via backspace
					if (e2.start + e2.text.length() == e1.start) {
						String text = e2.text + e1.text;
						if (leadingSpaces.matcher(text).matches())
							doMergeLastUndos(new Edit(e2.start, 0, text));
					}
					// Case 3: A continuous sequence of characters was deleted via the delete key
					else if (e1.start == e2.start) {
						String text = e1.text + e2.text;
						if (trailingSpaces.matcher(text).matches())
							doMergeLastUndos(new Edit(e2.start, 0, text));
					}
				}
			}
			private void doMergeLastUndos(Edit mergedUndo) {
				undoStack.removeLast();
				undoStack.removeLast();
				undoStack.add(mergedUndo);
			}
		};
		st.addExtendedModifyListener(modifyListener);
		
		st.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode != 'z') return;
				if ((e.stateMask & SWT.MOD1) == 0) return;
				if ((e.stateMask & SWT.SHIFT) == 0) {
					applyEdit(undoStack, redoStack);
				} else {
					applyEdit(redoStack, undoStack);
				}
			}
			private void applyEdit(LinkedList<Edit> fromStack, LinkedList<Edit> toStack) {
				if (fromStack.isEmpty()) return;
				Edit edit = fromStack.removeLast();
				String replacedText = st.getTextRange(edit.start, edit.length);
				st.removeExtendedModifyListener(modifyListener);
				st.replaceTextRange(edit.start, edit.length, edit.text);
				st.addExtendedModifyListener(modifyListener);
				toStack.add(new Edit(edit.start, edit.text.length(), replacedText));
				st.setCaretOffset(edit.start + edit.text.length());
			}
		});
	}
	
	private static class Edit {
		private int start;
		private int length;
		private String text;
		
		public Edit(int start, int length, String text) {
			this.start = start;
			this.length = length;
			this.text = text;
		}
	}

}
