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

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.model.search.HighlightedString;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * @author Tran Nam Quang
 */
final class TextPreview extends ToolBarForm {
	
	@NotNull private HighlightingText textViewer;

	public TextPreview(@NotNull Composite parent) {
		super(parent);
	}
	
	@NotNull
	protected Control createToolBar(@NotNull Composite parent) {
		Label label = new Label(parent, SWT.SINGLE | SWT.BORDER);
		label.setText("Toolbar");
		return label;
	}
	
	@NotNull
	protected Control createContents(@NotNull Composite parent) {
		// TODO put text viewer into a container with margins
		textViewer = new HighlightingText(parent);
		return textViewer.getControl();
	}
	
	public void clear() {
		textViewer.clear();
	}
	
	public void setText(@NotNull HighlightedString string) {
		textViewer.setText(string);
	}

	public void appendText(@NotNull HighlightedString string) {
		textViewer.appendText(string);
	}

}
