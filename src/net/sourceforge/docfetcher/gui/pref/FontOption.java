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

package net.sourceforge.docfetcher.gui.pref;

import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SettingsConf.FontDescription;
import net.sourceforge.docfetcher.gui.pref.PrefDialog.PrefOption;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;

/**
 * @author Tran Nam Quang
 */
final class FontOption extends PrefOption {
	
	private final FontDescription fontDescription;
	@NotNull private StyledLabel st;
	@NotNull private Font font;
	private int fontHeight;
	
	public FontOption(	@NotNull String labelText,
						@NotNull SettingsConf.FontDescription fontDescription) {
		super(labelText);
		this.fontDescription = fontDescription;
	}
	
	protected void createControls(Composite parent) {
		st = PrefDialog.createLabeledStyledLabel(parent, labelText);
		st.setCursor(st.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		setFont(fontDescription.createFontData());
		
		st.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				font.dispose();
			}
		});
		
		st.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				FontDialog dialog = new FontDialog(st.getShell());
				FontData[] oldFontData = font.getFontData();
				oldFontData[0].setHeight(fontHeight);
				dialog.setFontList(new FontData[] {oldFontData[0]});
				FontData newFontData = dialog.open();
				if (newFontData == null)
					return;
				Font oldFont = font;
				setFont(newFontData);
				oldFont.dispose();
			}
		});
	}
	
	private void setFont(@NotNull FontData fontData) {
		fontHeight = fontData.getHeight();
		Display display = st.getDisplay();
		Font systemFont = display.getSystemFont();
		fontData.setHeight(systemFont.getFontData()[0].getHeight());
		st.setFont(font = new Font(display, fontData));
		st.setText(fontData.getName() + " " + fontHeight);
	}
	
}