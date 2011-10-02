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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.GroupWrapper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
final class FileConfigPanel extends ConfigPanel {
	
	@NotNull private Text textExtField;
	@NotNull private Text zipExtField;
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
		targetField.setText(Util.getSystemAbsPath(config.getRootFile()));
		targetField.setForeground(Col.DARK_GRAY.get());
		targetField.setBackground(Col.WIDGET_BACKGROUND.get());
		UtilGui.clearSelectionOnFocusLost(targetField);
		
		Label targetSeparator = new Label(targetComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		setGridData(targetSeparator, false);
		
		Group extGroup = new GroupWrapper(comp, "File extensions") {
			protected void createLayout(Group parent) {
				GridLayout gridLayout = Util.createGridLayout(3, false, 7, 0);
				gridLayout.verticalSpacing = 5;
				parent.setLayout(gridLayout);
			}
			protected void createContents(Group parent) {
				textExtField = createExtField(
					parent, "Plain text:", config.getTextExtensions(),
					new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							// TODO now: implement
						}
					});
				zipExtField = createExtField(
					parent, "Zip archives:", config.getZipExtensions(),
					new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							// TODO now: implement
						}
					});
			}
			@NotNull
			private Text createExtField(@NotNull Composite parent,
										@NotNull String label,
										@NotNull Collection<String> extensions,
										@NotNull SelectionListener listener) {
				Text field = Util.createLabeledGridText(parent, label);
				((GridData)field.getLayoutData()).horizontalIndent = 5;
				field.setText(Util.join(" ", extensions));
				Util.createPushButton(parent, "...", listener).setLayoutData(
					new GridData(SWT.FILL, SWT.FILL, false, true));
				return field;
			}
		}.getGroup();
		
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
	
	protected void writeToConfig() {
		// TODO now: Check if target directory still exists
//		if (! scope.getFile().exists()) {
//			UtilGUI.showErrorMsg(Msg.target_folder_deleted.value());
//			return false;
//		}
		
		/*
		 * Validate all entered regexes and split them into filter and mime
		 * detection patterns.
		 */
		List<PatternAction> patternActions = patternTable.getPatternActions();
		for (PatternAction patternAction : patternActions) {
			if (!patternAction.validateRegex()) {
				// TODO now: show warning 'not_a_regex' and return
				return;
			}
		}
		
		config.setTextExtensions(getExtensions(textExtField));
		config.setZipExtensions(getExtensions(zipExtField));
		config.setPatternActions(patternActions);
		
		config.setHtmlPairing(htmlPairingBt.getSelection());
		config.setDetectExecutableArchives(detectExecArchivesBt.getSelection());
		config.setIndexFilenames(indexFilenameBt.getSelection());
		config.setStoreRelativePaths(storeRelativePathsBt.getSelection());
		config.setWatchFolders(watchFolderBt.getSelection());
	}
	
	@NotNull
	private static Collection<String> getExtensions(@NotNull Text text) {
		return Arrays.asList(text.getText().trim().split("[^\\\\p{Alnum}]+"));
	}
	
}
