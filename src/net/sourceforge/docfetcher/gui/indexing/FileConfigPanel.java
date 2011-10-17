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

import java.util.Collection;
import java.util.List;

import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.model.index.PatternAction.MatchTarget;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.collect.LazyList;
import net.sourceforge.docfetcher.util.gui.GroupWrapper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

/**
 * @author Tran Nam Quang
 */
final class FileConfigPanel extends ConfigPanel {
	
	@NotNull private FileExtensionGroupWrapper extGroupWrapper;
	@NotNull private PatternTable patternTable;
	@NotNull private Button htmlPairingBt;
	@NotNull private Button detectExecArchivesBt;
	@NotNull private Button indexFilenameBt;
	@NotNull private Button storeRelativePathsBt;
	@NotNull private Button watchFolderBt;
	
	public FileConfigPanel(	@NotNull Composite parent,
	                       	@NotNull LuceneIndex index) {
		super(parent, index, true);
	}
	
	protected Control createContents(Composite parent) {
		// TODO i18n
		Composite comp = new Composite(parent, SWT.NONE);
		
		extGroupWrapper = new FileExtensionGroupWrapper(comp, index);
		Group extGroup = extGroupWrapper.getGroup();
		
		Group patternGroup = new GroupWrapper(comp, "Exclude files / detect mime type") {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createFillLayout(5));
			}
			protected void createContents(Group parent) {
				patternTable = new PatternTable(parent, index);
			}
		}.getGroup();
		
		Group miscGroup = new GroupWrapper(comp, "Miscellaneous") {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createGridLayout(1, false, 3, 3));
			}
			protected void createContents(Group parent) {
				createMiscGroupContents(parent);
			}
		}.getGroup();
		
		GridLayout gridLayout = Util.createGridLayout(1, false, 0, 10);
		gridLayout.marginTop = 5;
		comp.setLayout(gridLayout);
		UtilGui.setGridData(extGroup, false);
		UtilGui.setGridData(patternGroup, false);
		UtilGui.setGridData(miscGroup, false);
		
		return comp;
	}
	
	private void createMiscGroupContents(@NotNull Group parent) {
		htmlPairingBt = Util.createCheckButton(parent, "ipref_detect_html_pairs");
		detectExecArchivesBt = Util.createCheckButton(parent, "Detect executable zip and 7z archives (slower)");
		indexFilenameBt = Util.createCheckButton(parent, "Index filename even if file contents can't be extracted");
		storeRelativePathsBt = Util.createCheckButton(parent, "Store relative paths if possible (for portability)");
		watchFolderBt = Util.createCheckButton(parent, "Watch folders for file changes");
		
		IndexingConfig config = index.getConfig();
		
		htmlPairingBt.setSelection(config.isHtmlPairing());
		detectExecArchivesBt.setSelection(config.isDetectExecutableArchives());
		indexFilenameBt.setSelection(config.isIndexFilenames());
		watchFolderBt.setSelection(config.isWatchFolders());
		
		boolean storeRelativePaths = config.isStoreRelativePaths();
		patternTable.setStoreRelativePaths(storeRelativePaths);
		storeRelativePathsBt.setSelection(storeRelativePaths);
		
		storeRelativePathsBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onStoreRelativePathsButtonClicked();
			}
		});
	}
	
	private void onStoreRelativePathsButtonClicked() {
		patternTable.setStoreRelativePaths(storeRelativePathsBt.getSelection());
		
		if (!SettingsConf.Bool.ShowRelativePathsMessage.get())
			return;
		
		for (PatternAction patternAction : patternTable.getPatternActions()) {
			if (patternAction.getTarget() != MatchTarget.PATH)
				continue;
			String msg = "Changing the 'store relative paths' setting might require adapting " +
					"some of the regular expressions in the pattern table that are matched against paths.";
			AppUtil.showInfo(msg);
			SettingsConf.Bool.ShowRelativePathsMessage.set(false);
			break;
		}
	}
	
	protected boolean writeToConfig() {
		// Check that the target file or directory still exists
		if (!index.getCanonicalRootFile().exists()) {
			AppUtil.showError("target_folder_deleted", true, true);
			return false;
		}
		
		// Validate the regexes
		List<PatternAction> patternActions = patternTable.getPatternActions();
		for (PatternAction patternAction : patternActions) {
			if (!patternAction.validateRegex()) {
				String regex = patternAction.getRegex();
				String msg = "Malformed regular expression: " + regex;
				AppUtil.showError(msg, true, true);
				return false;
			}
		}
		
		Collection<String> textExtensions = extGroupWrapper.getTextExtensions();
		
		String msg = "You've entered the following plain text extensions: %s. " +
		"This will override DocFetcher's built-in support for files with these extensions, and the files will instead be treated as simple text files." +
		"\n\nThis is probably not what you want because the built-in support will generally give better text extraction results. Do you still want " +
		"to continue?";
		if (!confirmExtensionOverride(textExtensions, msg))
			return false;
		
		Collection<String> zipExtensions = extGroupWrapper.getZipExtensions();
		
		msg = "You've entered the following zip extensions: %s. " +
		"This will override DocFetcher's built-in support for files with these extensions, and the files will instead be treated as zip archives. " +
		"Do you still want to continue?";
		if (!confirmExtensionOverride(zipExtensions, msg))
			return false;
		
		IndexingConfig config = index.getConfig();
		
		config.setTextExtensions(textExtensions);
		config.setZipExtensions(zipExtensions);
		config.setPatternActions(patternActions);
		
		config.setHtmlPairing(htmlPairingBt.getSelection());
		config.setDetectExecutableArchives(detectExecArchivesBt.getSelection());
		config.setIndexFilenames(indexFilenameBt.getSelection());
		config.setStoreRelativePaths(storeRelativePathsBt.getSelection());
		config.setWatchFolders(watchFolderBt.getSelection());
		
		return true;
	}
	
	// given message must have one %s placeholder for inserting the overriding file extensions
	private boolean confirmExtensionOverride(	@NotNull Collection<String> extensions,
												@NotNull String message) {
		LazyList<String> overridingExtensions = new LazyList<String>();
		for (String extension : extensions)
			if (ParseService.isBuiltInExtension(index.getConfig(), extension))
				if (!overridingExtensions.contains(extension))
					overridingExtensions.add(extension);
		
		if (!overridingExtensions.isEmpty()) {
			message = String.format(message, Util.join(", ", overridingExtensions));
			if (!AppUtil.showConfirmation(message, true))
				return false;
		}
		return true;
	}
	
	protected void restoreDefaults() {
		IndexingConfig config = index.getConfig();
		
		extGroupWrapper.setTextExtensions(config.getTextExtensions());
		extGroupWrapper.setZipExtensions(config.getZipExtensions());
		patternTable.restoreDefaults();
		
		htmlPairingBt.setSelection(config.isHtmlPairing());
		detectExecArchivesBt.setSelection(config.isDetectExecutableArchives());
		indexFilenameBt.setSelection(config.isIndexFilenames());
		storeRelativePathsBt.setSelection(config.isStoreRelativePaths());
		watchFolderBt.setSelection(config.isWatchFolders());
	}
	
}
