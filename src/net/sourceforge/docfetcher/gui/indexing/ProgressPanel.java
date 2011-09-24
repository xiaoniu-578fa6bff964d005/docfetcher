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

package net.sourceforge.docfetcher.gui.indexing;

import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

/**
 * @author Tran Nam Quang
 */
final class ProgressPanel {
	
	private final SashForm sash;
	private final ProgressTable progressTable;
	private final ErrorTable errorTable;

	public ProgressPanel(@NotNull Composite parent) {
		sash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
		
		// TODO i18n
		Group topGroup = new Group(sash, SWT.SHADOW_OUT);
		topGroup.setText("progress");
		topGroup.setLayout(Util.createFillLayout(1));
		Group bottomGroup = new Group(sash, SWT.SHADOW_OUT);
		bottomGroup.setText("errors");
		bottomGroup.setLayout(Util.createFillLayout(1));
		
		SettingsConf.SashWeights.ProgressPanel.bind(sash);
		
		int itemLimit = ProgramConf.Int.MaxLinesInProgressPanel.get();
		progressTable = new ProgressTable(topGroup, itemLimit);
		errorTable = new ErrorTable(bottomGroup);
	}
	
	@NotNull
	public Control getControl() {
		return sash;
	}
	
	@NotNull
	public ProgressTable getProgressTable() {
		return progressTable;
	}
	
	@NotNull
	public ErrorTable getErrorTable() {
		return errorTable;
	}

}
