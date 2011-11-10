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
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.collect.ListMap;
import net.sourceforge.docfetcher.util.collect.ListMap.Entry;
import net.sourceforge.docfetcher.util.gui.GroupWrapper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
final class FileExtensionGroupWrapper {
	
	@NotNull private Text textExtField;
	@NotNull private Text zipExtField;
	
	private final LuceneIndex index;
	private final FileExtensionChooser.Factory extChooserFactory;
	private final GroupWrapper groupWrapper;

	public FileExtensionGroupWrapper(	@NotNull Composite parent,
										@NotNull LuceneIndex index) {
		this.index = index;
		extChooserFactory = new FileExtensionChooser.Factory(
			parent.getShell(), index.getCanonicalRootFile());
		
		groupWrapper = new GroupWrapper(parent, Msg.file_extensions.get()) {
			protected void createLayout(Group parent) {
				FileExtensionGroupWrapper.this.createLayout(parent);
			}
			protected void createContents(Group parent) {
				FileExtensionGroupWrapper.this.createContents(parent);
			}
		};
	}
	
	@NotNull
	public Group getGroup() {
		return groupWrapper.getGroup();
	}

	private void createLayout(Group parent) {
		GridLayout gridLayout = Util.createGridLayout(3, false, 7, 0);
		gridLayout.verticalSpacing = 5;
		parent.setLayout(gridLayout);
	}

	private void createContents(Group parent) {
		textExtField = createExtField(
			parent, Msg.plain_text.get(), index.getConfig().getTextExtensions());
		zipExtField = createExtField(
			parent, Msg.zip_archives.get(), index.getConfig().getZipExtensions());
	}
	
	@NotNull
	private Text createExtField(@NotNull Composite parent,
								@NotNull String label,
								@NotNull Collection<String> extensions) {
		final Text field = Util.createLabeledGridText(parent, label);
		((GridData)field.getLayoutData()).horizontalIndent = 5;
		field.setText(Util.join(" ", extensions));
		
		Button chooserBt = Util.createPushButton(parent, "...", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onChooserButtonClicked(field);
			}
		});
		
		chooserBt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		return field;
	}
	
	private void onChooserButtonClicked(@NotNull final Text field) {
		File rootFile = index.getCanonicalRootFile();
		if (rootFile.isFile()) {
			AppUtil.showError(Msg.listing_ext_inside_archives.get(), true, true);
			return;
		}
		
		FileExtensionChooser chooser = extChooserFactory.createChooser();
		try {
			Collection<String> extsOld = getExtensions(field);
			ListMap<String, Boolean> extsFromTable = chooser.open(extsOld);
			if (extsFromTable == null)
				return;
			Set<String> extsNew = new TreeSet<String>(extsOld);
			for (Entry<String, Boolean> entry : extsFromTable) {
				if (entry.getValue())
					extsNew.add(entry.getKey());
				else
					extsNew.remove(entry.getKey());
			}
			field.setText(Util.join(" ", extsNew));
		}
		catch (FileNotFoundException e1) {
			String msg = Msg.folder_not_found.format(rootFile.getPath());
			AppUtil.showError(msg, true, true);
		}
	}
	
	@NotNull
	public Collection<String> getTextExtensions() {
		return getExtensions(textExtField);
	}
	
	public void setTextExtensions(@NotNull Collection<String> textExtensions) {
		textExtField.setText(Util.join(" ", textExtensions));
	}
	
	public void setZipExtensions(Collection<String> zipExtensions) {
		zipExtField.setText(Util.join(" ", zipExtensions));
	}
	
	@NotNull
	public Collection<String> getZipExtensions() {
		return getExtensions(zipExtField);
	}
	
	@Immutable
	@NotNull
	private static Collection<String> getExtensions(@NotNull Text text) {
		String string = text.getText().trim();
		
		/*
		 * Without this, a list containing an empty string would be returned,
		 * which will crash TrueZIP.
		 */
		if (string.isEmpty())
			return Collections.emptyList();
		
		return Arrays.asList(string.split("[^\\p{Alnum}]+"));
	}

}
