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

package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class UtilGui {
	
	private UtilGui() {
	}
	
	static final int OPEN_LIMIT = 10;
	
	/**
	 * Paints a border around the given Control. This can be used as a
	 * replacement for the ugly native border of Composites with SWT.BORDER
	 * style on Windows with classic theme turned on.
	 */
	public static void paintBorder(final Control control) {
		control.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				Point size = control.getSize();
				e.gc.setForeground(Col.WIDGET_NORMAL_SHADOW.get());
				e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);
				e.gc.setForeground(Col.WHITE.get());
				e.gc.drawRectangle(1, 1, size.x - 3, size.y - 3);
			}
		});
	}

	/**
	 * Attaches a focus listener to the given StyledText that clears the
	 * selection and resets the caret position when the StyledText looses focus.
	 */
	public static void clearSelectionOnFocusLost(@NotNull final StyledText styledText) {
		styledText.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				styledText.setSelection(0);
			}
		});
	}

}
