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

import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.collect.LazyList;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.GroupWrapper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

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
	                       	@NotNull IndexingConfig config) {
		super(parent, config);
	}
	
	protected Control createContents(Composite parent) {
		// TODO i18n
		Composite comp = new Composite(parent, SWT.NONE);
		
		Composite targetComp = new Composite(comp, SWT.NONE);
		targetComp.setLayout(Util.createGridLayout(1, false, 0, 0));
		
		int targetStyle = SWT.SINGLE | SWT.READ_ONLY;
		StyledText targetField = new StyledText(targetComp, targetStyle);
		setGridData(targetField, true);
		targetField.setText(config.getAbsoluteRootFile().getPath());
		targetField.setForeground(Col.DARK_GRAY.get());
		targetField.setBackground(Col.WIDGET_BACKGROUND.get());
		UtilGui.clearSelectionOnFocusLost(targetField);
		
		Label targetSeparator = new Label(targetComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		setGridData(targetSeparator, false);
		
		extGroupWrapper = new FileExtensionGroupWrapper(comp, config);
		Group extGroup = extGroupWrapper.getGroup();
		
		Group patternGroup = new GroupWrapper(comp, "Exclude files / detect mime type") {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createFillLayout(5));
			}
			protected void createContents(Group parent) {
				patternTable = new PatternTable(parent, config) {
					protected boolean storeRelativePaths() {
						return storeRelativePathsBt.getSelection();
					}
				};
			}
		}.getGroup();
		
		Group miscGroup = new GroupWrapper(comp, "Miscellaneous") {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createGridLayout(1, false, 3, 3));
			}
			protected void createContents(Group parent) {
				htmlPairingBt = Util.createCheckButton(parent, "ipref_detect_html_pairs");
				detectExecArchivesBt = Util.createCheckButton(parent, "Detect executable zip and 7z archives (slower)");
				indexFilenameBt = Util.createCheckButton(parent, "Index filename even if file contents can't be extracted");
				storeRelativePathsBt = Util.createCheckButton(parent, "Store relative paths if possible (for portability)");
				watchFolderBt = Util.createCheckButton(parent, "Watch folders for file changes");
				
				htmlPairingBt.setSelection(config.isHtmlPairing());
				detectExecArchivesBt.setSelection(config.isDetectExecutableArchives());
				indexFilenameBt.setSelection(config.isIndexFilenames());
				storeRelativePathsBt.setSelection(config.isStoreRelativePaths());
				watchFolderBt.setSelection(config.isWatchFolders());
			}
		}.getGroup();
		
		GridLayout gridLayout = Util.createGridLayout(1, false, 0, 10);
		gridLayout.marginTop = 5;
		comp.setLayout(gridLayout);
		setGridData(targetComp, false);
		setGridData(extGroup, false);
		setGridData(patternGroup, false);
		setGridData(miscGroup, false);
		
		return comp;
	}
	
	private static void setGridData(@NotNull Control control,
									boolean grabExcessVerticalSpace) {
		control.setLayoutData(new GridData(
			SWT.FILL, SWT.FILL, true, grabExcessVerticalSpace));
	}
	
	protected boolean writeToConfig() {
		// Check that the target file or directory still exists
		if (!config.getAbsoluteRootFile().exists()) {
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
			if (ParseService.isBuiltInExtension(config, extension))
				if (!overridingExtensions.contains(extension))
					overridingExtensions.add(extension);
		
		if (!overridingExtensions.isEmpty()) {
			message = String.format(message, Util.join(", ", overridingExtensions));
			if (!AppUtil.showConfirmation(message, true))
				return false;
		}
		return true;
	}
	
}
