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
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.ConfigComposite;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Tran Nam Quang
 */
abstract class ConfigPanel {

	public final Event<Void> evtRunButtonClicked = new Event<Void>();
	
	private final Composite comp;
	protected final LuceneIndex index;
	@NotNull private Button runBt;

	public ConfigPanel(@NotNull Composite parent,
	                   @NotNull LuceneIndex index) {
		Util.checkNotNull(index);
		this.index = index;
		
		// Must use composition rather than inheritance here because the subclasses
		// need the index object while creating their widgets
		comp = new ConfigComposite(parent, SWT.V_SCROLL) {
			protected Control createContents(Composite parent) {
				return ConfigPanel.this.createContents(parent);
			};
			protected Control createButtonArea(Composite parent) {
				return ConfigPanel.this.createButtonArea(parent);
			}
			public final boolean setFocus() {
				return runBt.setFocus();
			}
		};
	}
	
	@NotNull
	public final Control getControl() {
		return comp;
	}
	
	@NotNull
	protected abstract Control createContents(@NotNull Composite parent);
	
	// returns success
	protected abstract boolean writeToConfig();
	
	protected abstract void restoreDefaults();
	
	protected final Control createButtonArea(Composite parent) {
		// TODO i18n
		Composite comp = new Composite(parent, SWT.NONE);
		
		Button helpBt = Util.createPushButton(comp, "help", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO now: implement
//				UtilFile.launch(Const.HELP_FILE_INDEXING);
			}
		});
		
		Button resetBt = Util.createPushButton(comp, "restore_defaults", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				restoreDefaults();
			}
		});
		
		runBt = Util.createPushButton(comp, "run", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (writeToConfig())
					evtRunButtonClicked.fire(null);
			}
		});
		
		comp.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).top().bottom().minWidth(75).applyTo(helpBt);
		fdf.left(helpBt).applyTo(resetBt);
		fdf.unleft().right().applyTo(runBt);
		
		return comp;
	}

}
