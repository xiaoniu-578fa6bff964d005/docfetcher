/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ToolBarForm extends Composite {
	
	@NotNull private final ToolBarFormHeader header;
	@NotNull private final Control contents;

	public ToolBarForm(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(Util.createGridLayout(1, false, 0, 0));
		header = new ToolBarFormHeader(this) {
			@Nullable
			protected Control createToolBar(Composite parent) {
				return ToolBarForm.this.createToolBar(parent);
			}
		};
		contents = createContents(this);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Nullable
	public final Image getImage() {
		return header.getImage();
	}

	public final void setImage(@Nullable Image image) {
		header.setImage(image);
	}

	@NotNull
	public final String getText() {
		return header.getText();
	}

	public final void setText(@NotNull String text) {
		header.setText(text);
	}
	
	@Nullable
	protected Control createToolBar(Composite parent) {
		return null;
	}
	
	@NotNull
	protected Control createContents(Composite parent) {
		return new Composite(parent, SWT.NONE);
	}
	
	public final void setContentsVisible(boolean isVisible) {
		if (isVisible == contents.isVisible()) return;
		contents.setVisible(isVisible);
		GridData gridData = (GridData) contents.getLayoutData();
		gridData.exclude = ! isVisible;
		layout();
	}
	
	public final boolean isContentsVisible() {
		return contents.isVisible();
	}

}
