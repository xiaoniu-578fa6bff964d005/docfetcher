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

package net.sourceforge.docfetcher.gui.pref;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.gui.ConfigComposite;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class PrefDialog {
	
	private final Shell shell;
	@NotNull private Button okBt;
	private final List<PrefOption> checkOptions;
	private final List<PrefOption> fieldOptions;
	
	public PrefDialog(@NotNull Shell parent) {
		Util.checkNotNull(parent);
		shell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.SHELL_TRIM);
		shell.setLayout(Util.createFillLayout(10));
		shell.setText(Msg.preferences.get());
		shell.setImage(Img.PREFERENCES.get());
		SettingsConf.ShellBounds.PreferencesDialog.bind(shell);
		
		checkOptions = Arrays.<PrefOption> asList(
			new CheckOption(
				Msg.pref_manual_on_startup.get(),
				SettingsConf.Bool.ShowManualOnStartup),

			new CheckOption(
				Msg.pref_use_or_operator.get(),
				SettingsConf.Bool.UseOrOperator),

			new CheckOption(
				Msg.pref_hide_in_systray.get(),
				SettingsConf.Bool.HideOnOpen),

			new CheckOption(
				Msg.pref_clear_search_history_on_exit.get(),
				SettingsConf.Bool.ClearSearchHistoryOnExit)

			// TODO post-release-1.1: Implement this; requires saving and restoring the tree expansion state
//			new CheckOption(
//				"Reset location filter on exit",
//				SettingsConf.Bool.ResetLocationFilterOnExit),
		);
		
		fieldOptions = Util.createList(1,
			new ColorOption(
				Msg.pref_highlight_color.get(),
				SettingsConf.IntArray.PreviewHighlighting),
				
			new FontOption(
				Msg.pref_font_normal.get(),
				UtilGui.getPreviewFontNormal()),
			
			new FontOption(
				Msg.pref_font_fixed_width.get(),
				UtilGui.getPreviewFontMono())
		);
		if (!Util.IS_MAC_OS_X)
			fieldOptions.add(new HotkeyOption(Msg.pref_hotkey.get()));
		
		new ConfigComposite(shell, SWT.H_SCROLL | SWT.V_SCROLL) {
			protected Control createContents(Composite parent) {
				return PrefDialog.this.createContents(parent);
			}
			protected Control createButtonArea(Composite parent) {
				return PrefDialog.this.createButtonArea(parent);
			}
		};
	}
	
	@NotNull
	private Control createContents(@NotNull Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(Util.createGridLayout(2, false, 0, 5));
		
		for (PrefOption checkOption : checkOptions)
			checkOption.createControls(comp);
		
		Label spacing = new Label(comp, SWT.NONE);
		GridData spacingGridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		spacingGridData.heightHint = 3;
		spacing.setLayoutData(spacingGridData);
		
		for (PrefOption fieldOption : fieldOptions)
			fieldOption.createControls(comp);
		
		return comp;
	}
	
	@NotNull
	private Control createButtonArea(@NotNull Composite parent) {
		// TODO i18n
		Composite comp = new Composite(parent, SWT.NONE);
		
		Button helpBt = Util.createPushButton(comp, Msg.help.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO now: implement
//				UtilFile.launch(Const.HELP_FILE_INDEXING);
			}
		});
		
		Button resetBt = Util.createPushButton(comp, Msg.restore_defaults.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (PrefOption checkOption : checkOptions)
					checkOption.restoreDefault();
				for (PrefOption fieldOption : fieldOptions)
					fieldOption.restoreDefault();
			}
		});
		
		okBt = Util.createPushButton(comp, Msg.ok.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (PrefOption checkOption : checkOptions)
					checkOption.save();
				for (PrefOption fieldOption : fieldOptions)
					fieldOption.save();
				shell.close();
			}
		});
		
		Button cancelBt = Util.createPushButton(comp, Msg.cancel.get(), new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		
		Button[] okCancelBts = Util.maybeSwapButtons(okBt, cancelBt);
		
		comp.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).top().bottom().minWidth(Util.BTW).applyTo(helpBt);
		fdf.left(helpBt, 5).applyTo(resetBt);
		fdf.unleft().right().applyTo(okCancelBts[1]);
		fdf.right(okCancelBts[1], -5).applyTo(okCancelBts[0]);
		
		return comp;
	}

	public void open() {
		okBt.setFocus();
		shell.open();
		while (!shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
	}
	
	@NotNull
	static StyledLabel createLabeledStyledLabel(@NotNull Composite parent,
												@NotNull String labelText) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		StyledLabel text = new StyledLabel(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	static abstract class PrefOption {
		protected final String labelText;

		public PrefOption(@NotNull String labelText) {
			this.labelText = labelText;
		}
		// Subclassers must set grid datas on the created controls, assuming
		// a two-column grid layout
		protected abstract void createControls(@NotNull Composite parent);
		protected abstract void restoreDefault();
		protected abstract void save();
	}

}
