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

package net.sourceforge.docfetcher.util.gui;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.eclipse.swt.graphics.Image;

public class MenuAction {
	
	@Nullable private final Image image;
	private final String label;

	public MenuAction(@NotNull String label) {
		this(null, label);
	}

	public MenuAction(@Nullable Image image, @NotNull String label) {
		this.image = image;
		this.label = Util.checkNotNull(label);
	}

	public void run() {
	}

	public boolean isEnabled() {
		return true;
	}

	@Nullable
	public final Image getImage() {
		return image;
	}

	@NotNull
	public final String getLabel() {
		return label;
	}
	
}