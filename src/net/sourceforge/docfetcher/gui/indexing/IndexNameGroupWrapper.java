/*******************************************************************************
 * Copyright (c) 2016 Roy van Lamoen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Roy van Lamoen - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.gui.indexing;

import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.GroupWrapper;

import java.io.File;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * @author Roy van Lamoen
 */
final class IndexNameGroupWrapper {

	@NotNull private Text indexNameField;

	private final LuceneIndex index;
	private final GroupWrapper groupWrapper;

	public IndexNameGroupWrapper(@NotNull Composite parent,
								 @NotNull LuceneIndex index) {
		this.index = index;
		groupWrapper = new GroupWrapper(parent, Msg.index_name.get()) {
			protected void createLayout(Group parent) {
				IndexNameGroupWrapper.this.createLayout(parent);
			}
			protected void createContents(Group parent) {
				IndexNameGroupWrapper.this.createContents(parent);
			}
		};
	}
	
	@NotNull
	public Group getGroup() {
		return groupWrapper.getGroup();
	}
	
	private void createLayout(Group parent) {
		GridLayout gridLayout = Util.createGridLayout(1, false, 7, 0);
		gridLayout.verticalSpacing = 5;
		parent.setLayout(gridLayout);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	private void createContents(Group parent) {

		File rootFile = index.getCanonicalRootFile();
		String stdName = (Util.getDefaultIndexName(rootFile));
		indexNameField = createNameField(
				parent, stdName );
	}
	
	@NotNull
	private Text createNameField(@NotNull Composite parent,
								 @NotNull String text) {
		final Text field = Util.createUnlabeledGridText(parent);
		field.setText(text);
		return field;
	}
	
	@NotNull
	public String getName() {
		return indexNameField.getText();
	}
	
	public void setName(String name){
		indexNameField.setText(name);
	}

}
