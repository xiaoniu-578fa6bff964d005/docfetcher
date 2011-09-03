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

import java.io.File;
import java.net.MalformedURLException;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * @author Tran Nam Quang
 */
final class HtmlPreview extends ToolBarForm {
	
	@NotNull private Browser browser;
	
	public HtmlPreview(@NotNull Composite parent) {
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
		// TODO Add program.conf entry to allow using Mozilla (SWT.MOZILLA) or WebKit browser
		// TODO Browser may not be available on the system -> will throw an SWTError
		return browser = new Browser(parent, SWT.BORDER);
	}
	
	// TODO maybe add HTML highlighting
	/**
	 * Sets the file to be displayed.
	 */
	public void setFile(@NotNull File file) {
		String path = Util.getSystemAbsPath(file);
		try {
			String url = file.toURI().toURL().toString();
			browser.setUrl(url);
		}
		catch (MalformedURLException e) {
			browser.setUrl(path);
		}
		// TODO show file path in location bar
	}

	public void clear() {
		// TODO
	}

}
