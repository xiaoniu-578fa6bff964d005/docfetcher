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

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * A composite that paints a custom border on Windows and has a FormLayout with
 * adjusted margins set on itself.
 * 
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public class CustomBorderComposite extends Composite {
	
	public CustomBorderComposite(@NotNull Composite parent) {
		super(parent, Util.IS_WINDOWS ? SWT.NONE : SWT.BORDER);
		if (Util.IS_WINDOWS)
			UtilGui.paintBorder(this);
		setLayout(Util.createFormLayout(Util.IS_WINDOWS ? 2 : 0));
	}

}
