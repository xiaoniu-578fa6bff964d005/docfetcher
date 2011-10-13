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

import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.GroupWrapper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

/**
 * @author Tran Nam Quang
 */
final class OutlookConfigPanel extends ConfigPanel {
	
	@NotNull private Button indexFilenameBt;
	@NotNull private Button storeRelativePathsBt;
	@NotNull private Button watchFolderBt;
	
	public OutlookConfigPanel(	@NotNull Composite parent,
								@NotNull final LuceneIndex index) {
		super(parent, index, false);
	}
	
	protected Control createContents(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		
		Group optionsGroup = new GroupWrapper(comp, "Indexing Options") {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createGridLayout(1, false, 3, 3));
			}
			protected void createContents(Group parent) {
				createGroupContents(parent);
			}
		}.getGroup();
		
		GridLayout gridLayout = Util.createGridLayout(1, false, 0, 0);
		gridLayout.marginTop = 10;
		gridLayout.marginBottom = 10;
		comp.setLayout(gridLayout);
		UtilGui.setGridData(optionsGroup, true);
		
		return comp;
	}
	
	private void createGroupContents(@NotNull Group parent) {
		indexFilenameBt = Util.createCheckButton(parent, "Index filename even if file contents can't be extracted");
		storeRelativePathsBt = Util.createCheckButton(parent, "Store relative paths if possible (for portability)");
		watchFolderBt = Util.createCheckButton(parent, "Watch folders for file changes");
		
		IndexingConfig config = index.getConfig();
		
		indexFilenameBt.setSelection(config.isIndexFilenames());
		watchFolderBt.setSelection(config.isWatchFolders());
		storeRelativePathsBt.setSelection(config.isStoreRelativePaths());
	}
	
	protected boolean writeToConfig() {
		// Check that the PST file still exists
		if (!index.getCanonicalRootFile().exists()) {
			AppUtil.showError("Outlook PST file has been deleted!", true, true);
			return false;
		}
		
		IndexingConfig config = index.getConfig();
		config.setIndexFilenames(indexFilenameBt.getSelection());
		config.setStoreRelativePaths(storeRelativePathsBt.getSelection());
		config.setWatchFolders(watchFolderBt.getSelection());
		return true;
	}
	
	protected void restoreDefaults() {
		IndexingConfig config = index.getConfig();
		indexFilenameBt.setSelection(config.isIndexFilenames());
		storeRelativePathsBt.setSelection(config.isStoreRelativePaths());
		watchFolderBt.setSelection(config.isWatchFolders());
	}

}
