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

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Tran Nam Quang
 */
abstract class ToolBarForm extends Composite {
	
	public ToolBarForm(@NotNull Composite parent) {
		super(parent, SWT.NONE);
		Control toolBar = createToolBar(this);
		Control contents = createContents(this);
		setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).top().left().right().applyTo(toolBar);
		fdf.top(toolBar).bottom().applyTo(contents);
	}
	
	@NotNull
	protected abstract Control createToolBar(@NotNull Composite parent);
	
	@NotNull
	protected abstract Control createContents(@NotNull Composite parent);

}
