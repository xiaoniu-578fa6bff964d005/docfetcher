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

import net.sourceforge.docfetcher.gui.indexing.PatternAction.MatchTarget;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
abstract class RegexTestPanel extends Composite {
	
	private final Label label;
	private final Text fileBox;
	private List<PatternAction> patternActions = Collections.emptyList();

	// TODO i18n
	
	public RegexTestPanel(	@NotNull Composite parent,
							@NotNull final IndexingConfig config) {
		super(parent, SWT.NONE);
		Util.checkNotNull(config);
		label = new Label(this, SWT.NONE);
		label.setText("regex_matches_file_no");
		fileBox = new Text(this, SWT.BORDER | SWT.SINGLE);
		
		Button fileChooserBt = Util.createPushButton(this, "...", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				dialog.setFilterPath(config.getRootFile().getPath());
				dialog.setText("choose_regex_testfile_title");
				String filepath = dialog.open();
				if (filepath == null)
					return;
				File file = new File(filepath);
				filepath = config.getStorablePath(file, storeRelativePaths());
				fileBox.setText(filepath);
			}
		});
		
		fileBox.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateLabel();
			}
		});
		
		setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.bottom().right().applyTo(fileChooserBt);
		fdf.margin(0).left(0, 5).right(fileChooserBt).applyTo(fileBox);
		fdf.reset().left().right().top().bottom(fileBox).applyTo(label);
	}
	
	public final void setPatternActions(@NotNull List<PatternAction> patternActions) {
		Util.checkNotNull(patternActions);
		this.patternActions = patternActions;
		updateLabel();
	}
	
	protected abstract boolean storeRelativePaths();
	
	private void updateLabel() {
		label.setText(matches()
			? "regex_matches_file_yes"
			: "regex_matches_file_no");
	}
	
	private boolean matches() {
		if (patternActions.isEmpty())
			return false;
		
		String filepath = fileBox.getText().trim();
		if (filepath.length() == 0)
			return false;
		String filename = new File(filepath).getName();
		
		for (PatternAction patternAction : patternActions) {
			try {
				Pattern pattern = Pattern.compile(patternAction.getRegex());
				String target = patternAction.getTarget() == MatchTarget.FILENAME
					? filename
					: filepath;
				if (pattern.matcher(target).matches())
					return true;
			}
			catch (PatternSyntaxException e) {
				// Ignore
			}
		}
		return false;
	}

}
