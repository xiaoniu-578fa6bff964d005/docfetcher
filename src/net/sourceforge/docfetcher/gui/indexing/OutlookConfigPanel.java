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

import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Tran Nam Quang
 */
final class OutlookConfigPanel extends ConfigPanel {
	
	public OutlookConfigPanel(	@NotNull Composite parent,
								@NotNull final IndexingConfig config) {
		super(parent);
		Util.checkNotNull(config);
		
		// TODO now: implement Outlook config panel
		setLayout(new RowLayout());
		Button bt = new Button(this, SWT.PUSH);
		bt.setText("Run");
		bt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO now: only fire event if configuration is valid
				evtRunButtonClicked.fire(config);
			}
		});
		setBackground(Col.CYAN.get());
	}

}
