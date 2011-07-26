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

package net.sourceforge.docfetcher.base.gui;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

import org.eclipse.swt.widgets.Shell;

public abstract class InputDialogLoop<T> extends InputLoop<T> {
	
	// TODO i18n
	public static String ENTER_NAME = "Please enter a new name:";
	
	@NotNull private final Shell parent;
	@NotNull private String shellTitle = "";
	@NotNull private String message = ENTER_NAME;
	private boolean selectFilenameOnly = false;
	
	public InputDialogLoop(@NotNull Shell parent) {
		this.parent = Util.checkNotNull(parent);
	}
	
	@NotNull
	public final String getShellTitle() {
		return shellTitle;
	}

	public final void setShellTitle(@NotNull String shellTitle) {
		this.shellTitle = Util.checkNotNull(shellTitle);
	}

	@NotNull
	public final String getMessage() {
		return message;
	}

	public final void setMessage(@NotNull String message) {
		this.message = Util.checkNotNull(message);
	}
	
	@Nullable
	protected final String getNewValue(@NotNull String lastValue) {
		InputDialog dialog = new InputDialog(parent, shellTitle, message, lastValue);
		dialog.setSelectFilenameOnly(selectFilenameOnly);
		return dialog.open();
	}

	public final boolean isSelectFilenameOnly() {
		return selectFilenameOnly;
	}

	public final void setSelectFilenameOnly(boolean selectFilenameOnly) {
		this.selectFilenameOnly = selectFilenameOnly;
	}

}
