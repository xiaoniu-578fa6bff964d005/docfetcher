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

import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Tran Nam Quang
 */
final class OutlookConfigPanel extends ConfigPanel {
	
	public OutlookConfigPanel(	@NotNull Composite parent,
								@NotNull final LuceneIndex index) {
		super(parent, index);
	}
	
	protected Control createContents(Composite parent) {
		// TODO now: implement Outlook config panel
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new RowLayout());
		Button bt = new Button(comp, SWT.PUSH);
		bt.setText("Run");
		bt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO now: only fire event if configuration is valid
				evtRunButtonClicked.fire(null);
			}
		});
		comp.setBackground(Col.CYAN.get());
		return comp;
	}
	
	protected boolean writeToConfig() {
		// TODO now: implement
		return true;
	}
	
	protected void restoreDefaults() {
		// TODO now: implement
	}

}
