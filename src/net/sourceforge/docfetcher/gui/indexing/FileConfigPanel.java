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

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
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
		Composite comp = new Composite(parent, SWT.NONE);
		
		extGroupWrapper = new FileExtensionGroupWrapper(comp, index);
		Group extGroup = extGroupWrapper.getGroup();
		
		Group patternGroup = new GroupWrapper(comp, Msg.exclude_files_detect_mime_type.get()) {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createFillLayout(5));
			}
			protected void createContents(Group parent) {
				patternTable = new PatternTable(parent, index);
			}
		}.getGroup();
		
		Group miscGroup = new GroupWrapper(comp, Msg.miscellaneous.get()) {
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
		htmlPairingBt = Util.createCheckButton(parent, Msg.index_html_pairs.get());
		detectExecArchivesBt = Util.createCheckButton(parent, Msg.detect_exec_archives.get());
		indexFilenameBt = Util.createCheckButton(parent, Msg.index_filenames.get());
		storeRelativePathsBt = Util.createCheckButton(parent, Msg.store_relative_paths.get());
		watchFolderBt = Util.createCheckButton(parent, Msg.watch_folders.get());
		
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
			AppUtil.showInfo(Msg.changing_store_relative_paths_setting.get());
			SettingsConf.Bool.ShowRelativePathsMessage.set(false);
			break;
		}
	}
	
	protected boolean writeToConfig() {
		// Validate the regexes
		List<PatternAction> patternActions = patternTable.getPatternActions();
		for (PatternAction patternAction : patternActions) {
			if (!patternAction.validateRegex()) {
				String regex = patternAction.getRegex();
				String msg = Msg.malformed_regex.format(regex);
				AppUtil.showError(msg, true, true);
				return false;
			}
		}
		
		Collection<String> textExtensions = extGroupWrapper.getTextExtensions();
		if (!confirmExtensionOverride(textExtensions, Msg.confirm_text_ext))
			return false;
		
		Collection<String> zipExtensions = extGroupWrapper.getZipExtensions();
		for (String zipExt : zipExtensions) {
			if (zipExt.matches("\\d.*")) {
				AppUtil.showError(Msg.zip_ext_digits.format(zipExt), true, true);
				return false;
			}
		}
		
		if (!confirmExtensionOverride(zipExtensions, Msg.confirm_zip_ext, "7z", "rar"))
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
		config.setSkipTarArchives(ProgramConf.Bool.SkipTarArchives.get());
		
		return true;
	}
	
	// given message must have one %s placeholder for inserting the overriding file extensions
	private boolean confirmExtensionOverride(	@NotNull Collection<String> extensions,
												@NotNull Msg msg,
												@NotNull String... extraBuiltInExts) {
		LazyList<String> overridingExtensions = new LazyList<String>();
		for (String extension : extensions) {
			if (ParseService.isBuiltInExtension(index.getConfig(), extension))
				if (!overridingExtensions.contains(extension))
					overridingExtensions.add(extension);
			
			extension = extension.toLowerCase();
			for (String extraExt : extraBuiltInExts) {
				if (extension.equals(extraExt)) {
					if (!overridingExtensions.contains(extension))
						overridingExtensions.add(extension);
					break;
				}
			}
		}
		
		if (!overridingExtensions.isEmpty()) {
			String message = msg.format(Util.join(", ", overridingExtensions));
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
