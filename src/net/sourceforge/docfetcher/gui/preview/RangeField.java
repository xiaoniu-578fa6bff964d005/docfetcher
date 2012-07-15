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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.gui.CustomBorderComposite;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Tran Nam Quang
 */
final class RangeField {
	
	public final Event<Integer> evtValueChanged = new Event<Integer>();
	
	private final Composite comp;
	private final StyledText st;
	@Nullable private Integer lastValue;
	@Nullable private Integer lastTotal;
	private boolean verifyInput = true;
	private boolean modifiedByUser = false;
	
	public RangeField(@NotNull Composite parent) {
		comp = new CustomBorderComposite(parent);
		int margin = Util.IS_WINDOWS ? 2 : 0;
		comp.setLayout(Util.createGridLayout(1, false, margin, 0));
		st = new StyledText(comp, SWT.SINGLE | SWT.CENTER);
		st.setForeground(Col.WIDGET_FOREGROUND.get());
		st.setBackground(Col.WIDGET_BACKGROUND.get());
		st.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Util.selectAllOnFocus(st);
		
		// Only allow entering digits and deleting existing characters
		st.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if (!verifyInput)
					return;
				e.doit = lastTotal != null && e.text.matches("\\d*");
			}
		});
		
		st.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (verifyInput)
					modifiedByUser = true;
			}
		});
		
		// Fire change event when new value was confirmed by pressing Enter
		st.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (lastTotal != null && Util.isEnterKey(e.keyCode)) {
					Integer value = getIntValue();
					if (value != null)
						evtValueChanged.fire(value);
					modifiedByUser = false;
				}
			}
		});
		
		// Reset field on focus lost
		st.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				modifiedByUser = false;
				setRange(lastValue, lastTotal);
				st.setSelection(0);
			}
		});
	}
	
	@Nullable
	private Integer getIntValue() {
		// This pattern allows expressions like "12 / 501" and is used to
		// extract the first number
		Pattern pattern = Pattern.compile("(\\d+)(?:\\s*/\\s*\\d+)?");
		Matcher m = pattern.matcher(st.getText());
		if (!m.matches())
			return null; // Entered value may be empty
		try {
			// User can't enter minus character into text field, so this integer
			// must be positive
			return Integer.parseInt(m.group(1));
		}
		catch (NumberFormatException e1) {
			// Entered value was too long
			return null;
		}
	}
	
	public void clear() {
		verifyInput = false;
		st.setText("");
		lastValue = null;
		lastTotal = null;
		verifyInput = true;
	}
	
	public void setRange(@Nullable Integer current, @Nullable Integer total) {
		verifyInput = false;
		/*
		 * If the user has entered something into the text field and the focus
		 * is still there, do not overwrite the contents. Also, preserve the
		 * text selection if possible.
		 */
		if (!modifiedByUser) {
			st.setRedraw(false);
			Point sel = getNonVisualSelection();
			if (total == null) {
				st.setText("");
			}
			else if (current == null) {
				st.setText(String.valueOf(total));
			}
			else {
				String currentStr = String.valueOf(current);
				st.setText(currentStr + " / " + total);
				st.setStyleRange(new StyleRange(0, currentStr.length(), null, null, SWT.BOLD));
			}
			st.setSelection(sel);
			st.setRedraw(true);
		}
		lastValue = current;
		lastTotal = total;
		verifyInput = true;
	}
	
	@NotNull
	public Control getControl() {
		return comp;
	}
	
	@NotNull
	private Point getNonVisualSelection() {
		Point s = st.getSelection();
		int c = st.getCaretOffset();
		if (c == s.x) {
			int tmp = s.x;
			s.x = s.y;
			s.y = tmp;
		}
		return s;
	}

}
