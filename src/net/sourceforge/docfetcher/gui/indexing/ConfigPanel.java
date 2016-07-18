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

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.gui.ManualLocator;
import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.ConfigComposite;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * @author Tran Nam Quang
 */
abstract class ConfigPanel {

	public final Event<Void> evtRunButtonClicked = new Event<Void>();
	
	private final Composite comp;
	protected final LuceneIndex index;
	@NotNull private Button runBt;

	protected ConfigPanel(	@NotNull Composite parent,
							@NotNull LuceneIndex index,
							boolean fillVertical) {
		Util.checkNotNull(index);
		this.index = index;
		
		comp = new Composite(parent, SWT.NONE);
		comp.setLayout(Util.createGridLayout(1, false, 5, 0));
		
		Composite targetComp = new Composite(comp, SWT.NONE);
		UtilGui.setGridData(targetComp, false);
		targetComp.setLayout(Util.createGridLayout(1, false, 0, 0));
		
		int targetStyle = SWT.SINGLE | SWT.READ_ONLY;
		StyledText targetField = new StyledText(targetComp, targetStyle);
		UtilGui.setGridData(targetField, true);
		targetField.setText(index.getCanonicalRootFile().getPath());
		targetField.setForeground(Col.DARK_GRAY.get());
		targetField.setBackground(Col.WIDGET_BACKGROUND.get());
		UtilGui.clearSelectionOnFocusLost(targetField);
		
		Label targetSeparator = new Label(targetComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		UtilGui.setGridData(targetSeparator, false);
		
		Control configComp = createConfigComposite(comp);
		GridData gridData = fillVertical
			? new GridData(SWT.FILL, SWT.FILL, true, true)
			: new GridData(SWT.FILL, SWT.TOP, true, false);
		configComp.setLayoutData(gridData);
	}
	
	@NotNull
	private Composite createConfigComposite(@NotNull Composite parent) {
		return new ConfigComposite(parent, SWT.V_SCROLL) {
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
		Composite comp = new Composite(parent, SWT.NONE);
		
		Button helpBt = Util.createPushButton(comp, Msg.help.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				File file = ManualLocator.getManualSubpageFile("Indexing_Options.html");
				if (file == null) {
					AppUtil.showError(Msg.file_not_found.get() + "\n" +
							"Indexing_Options.html", true, false);
				} else {
					Util.launch(file);
				}
			}
		});
		
		Button resetBt = Util.createPushButton(comp, Msg.restore_defaults.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				restoreDefaults();
			}
		});
		
		runBt = Util.createPushButton(comp, Msg.run.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Check that the target file or directory still exists
				File rootFile = index.getCanonicalRootFile();
				if (!rootFile.exists()) {
					String msg = Msg.file_or_folder_not_found.get() + "\n" + rootFile.getPath();
					AppUtil.showError(msg, true, true);
					return;
				}
				
				if (writeToConfig())
					evtRunButtonClicked.fire(null);
			}
		});
		
		comp.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).top().bottom().left().minWidth(Util.BTW).applyTo(helpBt);
		fdf.left(helpBt).applyTo(resetBt);
		fdf.unleft().right().applyTo(runBt);
		
		return comp;
	}

}
