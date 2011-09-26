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
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;
import net.sourceforge.docfetcher.util.gui.GroupWrapper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
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
	
	@NotNull private Button useRelativePathsBt;
	private final Button runBt;

	public FileConfigPanel(	@NotNull Composite parent,
	                       	@NotNull final IndexingConfig config) {
		super(parent);
		Util.checkNotNull(config);
		
		// TODO i18n
		
		Composite targetComp = new Composite(this, SWT.NONE);
		targetComp.setLayout(Util.createGridLayout(1, false, 0, 0));
		
		int targetStyle = SWT.SINGLE | SWT.READ_ONLY;
		StyledText targetField = new StyledText(targetComp, targetStyle);
		setGridData(targetField, true, true);
		targetField.setText(Util.getSystemAbsPath(config.getRootFile()));
		targetField.setForeground(Col.DARK_GRAY.get());
		targetField.setBackground(Col.WIDGET_BACKGROUND.get());
		UtilGui.clearSelectionOnFocusLost(targetField);
		
		Label targetSeparator = new Label(targetComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		setGridData(targetSeparator, true, false);
		
		Group extGroup = new GroupWrapper(this, "File extensions") {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createGridLayout(2, false, 7, 7));
			}
			protected void createContents(Group parent) {
				Text textExtField = Util.createLabeledGridText(parent, "Plain text:");
				Text zipExtField = Util.createLabeledGridText(parent, "Zip archives:");
				textExtField.setText(Util.join(" ", config.getTextExtensions()));
				zipExtField.setText(Util.join(" ", config.getZipExtensions()));
			}
		}.getGroup();
		
		Group patternGroup = new GroupWrapper(this, "Exclude files / detect mime type") {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createFillLayout(5));
			}
			protected void createContents(Group parent) {
				new PatternTable(parent, config) {
					protected boolean useRelativePaths() {
						return useRelativePathsBt.getSelection();
					}
				};
				// TODO initialize pattern table from config object
			}
		}.getGroup();
		
		Group miscGroup = new GroupWrapper(this, "Miscellaneous") {
			protected void createLayout(Group parent) {
				parent.setLayout(Util.createGridLayout(1, false, 3, 3));
			}
			protected void createContents(Group parent) {
				Button htmlPairingBt = Util.createCheckButton(parent, "ipref_detect_html_pairs");
				useRelativePathsBt = Util.createCheckButton(parent, "Use relative paths if possible");
				Button watchFolderBt = Util.createCheckButton(parent, "Watch folder for file changes");
				Button detectExecArchivesBt = Util.createCheckButton(parent, "Detect executable zip and 7z archives (slower)");
				
				htmlPairingBt.setSelection(config.isHtmlPairing());
				useRelativePathsBt.setSelection(config.isUseRelativePaths());
				watchFolderBt.setSelection(config.isWatchFolders());
//				detectExecArchivesBt.setSelection(...) // TODO
			}
		}.getGroup();
		
		Composite buttonComp = new Composite(this, SWT.NONE);
		
		Button helpBt = Util.createPushButton(buttonComp, "help", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO now: implement
			}
		});
		
		Button resetBt = Util.createPushButton(buttonComp, "restore_defaults", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO now: implement
			}
		});
		
		runBt = Util.createPushButton(buttonComp, "run", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtRunButtonClicked.fire(config);
			}
		});
		
		Label blankSpaceBottom = new Label(this, SWT.NONE);
		
		GridLayout gridLayout = Util.createGridLayout(1, false, 0, 10);
		gridLayout.marginTop = 5;
		setLayout(gridLayout);
		setGridData(targetComp, true, false);
		setGridData(extGroup, true, false);
		setGridData(patternGroup, true, false);
		setGridData(miscGroup, true, false);
		setGridData(buttonComp, true, false);
		setGridData(blankSpaceBottom, true, true);
		
		buttonComp.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top().bottom().margin(0).minWidth(75).applyTo(helpBt);
		fdf.left(helpBt).applyTo(resetBt);
		fdf.unleft().right().applyTo(runBt);
	}
	
	private static void setGridData(@NotNull Control control,
									boolean grabExcessHorizontalSpace,
									boolean grabExcessVerticalSpace) {
		control.setLayoutData(new GridData(
			SWT.FILL, SWT.FILL, grabExcessHorizontalSpace,
			grabExcessVerticalSpace));
	}
	
	public boolean setFocus() {
		return runBt.setFocus();
	}

}
