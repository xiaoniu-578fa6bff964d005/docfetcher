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

import net.sourceforge.docfetcher.gui.CustomBorderComposite;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Tran Nam Quang
 */
final class RangeField {
	
	private final Composite comp;
	private final StyledText st;
	
	public RangeField(@NotNull Composite parent, boolean editable) {
		comp = new CustomBorderComposite(parent);
		int margin = Util.IS_WINDOWS ? 2 : 0;
		comp.setLayout(Util.createGridLayout(1, false, margin, 0));
		int style = SWT.SINGLE | SWT.CENTER;
		if (!editable)
			style |= SWT.READ_ONLY;
		st = new StyledText(comp, style);
		st.setForeground(Col.WIDGET_FOREGROUND.get());
		st.setBackground(Col.WIDGET_BACKGROUND.get());
		st.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
	}
	
	public void clear() {
		st.setText("");
	}
	
	public void setRange(@Nullable Integer current, int total) {
		if (current == null) {
			st.setText(String.valueOf(total));
		}
		else {
			String currentStr = String.valueOf(current);
			st.setText(currentStr + " / " + total);
			st.setStyleRange(new StyleRange(0, currentStr.length(), null, null, SWT.BOLD));
		}
	}
	
	@NotNull
	public Control getControl() {
		return comp;
	}

}
