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

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Tran Nam Quang
 */
public final class ToolItemFactory {
	
	private final ToolBar toolBar;
	@Nullable private Image image;
	@Nullable private String text;
	@Nullable private String toolTip;
	@Nullable private SelectionListener selectionListener;
	private boolean enabled = true;

	public ToolItemFactory(@NotNull ToolBar toolBar) {
		Util.checkNotNull(toolBar);
		this.toolBar = toolBar;
	}
	
	@NotNull
	public ToolItem create() {
		ToolItem item = new ToolItem(toolBar, SWT.PUSH);
		if (image != null)
			item.setImage(image);
		if (text != null)
			item.setText(text);
		if (toolTip != null)
			item.setToolTipText(toolTip);
		if (selectionListener != null)
			item.addSelectionListener(selectionListener);
		if (!enabled)
			item.setEnabled(false);
		return item;
	}
	
	@NotNull
	public ToolItemFactory image(@Nullable Image image) {
		this.image = image;
		return this;
	}
	
	@NotNull
	public ToolItemFactory text(@Nullable String text) {
		this.text = text;
		return this;
	}
	
	@NotNull
	public ToolItemFactory toolTip(@Nullable String toolTip) {
		this.toolTip = toolTip;
		return this;
	}
	
	@NotNull
	public ToolItemFactory listener(@Nullable SelectionListener selectionListener) {
		this.selectionListener = selectionListener;
		return this;
	}
	
	@NotNull
	public ToolItemFactory enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

}
