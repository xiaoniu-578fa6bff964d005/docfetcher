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
import net.sourceforge.docfetcher.gui.pref.PrefDialog.PrefOption;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Tran Nam Quang
 */
final class ColorOption extends PrefOption {
	
	@NotNull private SettingsConf.IntArray enumOption;
	@NotNull private StyledText st;
	@NotNull private Color color;
	
	public ColorOption(	@NotNull String labelText,
						@NotNull SettingsConf.IntArray enumOption) {
		super(labelText);
		this.enumOption = enumOption;
	}
	
	protected void createControls(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		int style = SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY;
		st = new StyledText(parent, style);
		st.setCaret(null);
		st.setCursor(st.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		
		GridData stGridData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		stGridData.widthHint = 50;
		st.setLayoutData(stGridData);
		setColor(enumOption.get());
		
		st.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				ColorDialog dialog = new ColorDialog(st.getShell());
				dialog.setRGB(color.getRGB());
				RGB rgb = dialog.open();
				if (rgb == null)
					return;
				Color oldColor = color;
				setColor(rgb.red, rgb.green, rgb.blue);
				oldColor.dispose();
			}
		});
		
		st.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				color.dispose();
			}
		});
	}
	
	private void setColor(@NotNull int... rgb) {
		color = new Color(st.getDisplay(), rgb[0], rgb[1], rgb[2]);
		st.setBackground(color);
		st.setSelectionBackground(color);
		st.setSelectionForeground(st.getForeground());
	}
	
}