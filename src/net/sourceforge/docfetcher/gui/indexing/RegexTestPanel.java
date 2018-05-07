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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.PatternAction;
import net.sourceforge.docfetcher.model.index.PatternAction.MatchTarget;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
final class RegexTestPanel extends Composite {
	
	private final Label label;
	private final Text fileBox;
	private List<PatternAction> patternActions = Collections.emptyList();
	private boolean storeRelativePaths;

	public RegexTestPanel(	@NotNull Composite parent,
							@NotNull final LuceneIndex index) {
		super(parent, SWT.NONE);
		Util.checkNotNull(index);
		label = new Label(this, SWT.NONE);
		label.setText("");
		fileBox = new Text(this, SWT.BORDER | SWT.SINGLE);
		
		Button fileChooserBt = Util.createPushButton(this, "...", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				dialog.setFilterPath(index.getCanonicalRootFile().getPath());
				dialog.setText(Msg.choose_regex_testfile_title.get());
				String filepath = dialog.open();
				if (filepath == null)
					return;
				File file = new File(filepath);
				Path newPath = IndexingConfig.getStorablePath(file, storeRelativePaths);
				fileBox.setText(newPath.getPath());
			}
		});
		
		fileBox.addKeyListener(new KeyAdapter(){
			public void keyReleased(KeyEvent e) {
				if (Util.isEnterKey(e.keyCode))
                    updateLabel();
				else
					clearLable();
			}
		});


		setLayout(Util.createGridLayout(2, false, 0, 0));
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 2, 1));
		fileBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fileChooserBt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
	}
	
	public void setPatternActions(@NotNull List<PatternAction> patternActions) {
		Util.checkNotNull(patternActions);
		this.patternActions = patternActions;
		updateLabel();
	}
	
	public void setStoreRelativePaths(boolean storeRelativePaths) {
		if (this.storeRelativePaths == storeRelativePaths)
			return;
		this.storeRelativePaths = storeRelativePaths;
		
		String path = fileBox.getText().trim();
		if (path.isEmpty())
			return;
		File file = Util.getCanonicalFile(path);
		Path newPath = IndexingConfig.getStorablePath(file, storeRelativePaths);
		fileBox.setText(newPath.getPath());
	}
	
	private void updateLabel() {
		fileBox.setText(new Path(fileBox.getText()).getPath());
		fileBox.setSelection(fileBox.getText().length());

		if(fileBox.getText().isEmpty()){
			clearLable();
			return;
		}

		try {
			label.setText(matches()
				? Msg.sel_regex_matches_file_yes.get()
				: Msg.sel_regex_matches_file_no.get());
		}
		catch (PatternSyntaxException e) {
			label.setText(Msg.sel_regex_malformed.get());
		}
	}
	private void clearLable() {
		label.setText("");
	}

	private boolean matches() throws PatternSyntaxException {
		if (patternActions.isEmpty())
			return false;
		
		String filepath = fileBox.getText().trim();
		if (filepath.length() == 0)
			return false;
		String filename = new File(filepath).getName();
		
		for (PatternAction patternAction : patternActions) {
			Pattern pattern = Pattern.compile(patternAction.getRegex());
			String target = patternAction.getTarget() == MatchTarget.FILENAME
				? filename
				: filepath;
			if (pattern.matcher(target).matches())
				return true;
		}
		return false;
	}

}
