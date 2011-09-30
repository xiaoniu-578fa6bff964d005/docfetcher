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

import java.util.List;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.indexing.PatternAction.MatchAction;
import net.sourceforge.docfetcher.gui.indexing.PatternAction.MatchTarget;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.LazyImageCache;
import net.sourceforge.docfetcher.util.gui.viewer.ColumnEditSupport;
import net.sourceforge.docfetcher.util.gui.viewer.ColumnEditSupport.ComboEditSupport;
import net.sourceforge.docfetcher.util.gui.viewer.ColumnEditSupport.TextEditSupport;
import net.sourceforge.docfetcher.util.gui.viewer.SimpleTableViewer;
import net.sourceforge.docfetcher.util.gui.viewer.SimpleTableViewer.Column;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

/**
 * @author Tran Nam Quang
 */
abstract class PatternTable extends Composite {
	
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());

		LazyImageCache lazyImageCache = new LazyImageCache(display, "dist/img");
		Img.initialize(lazyImageCache);
		AppUtil.Const.autoInit();
		
		final IndexingConfig config = new IndexingConfig();
		
		new PatternTable(shell, config) {
			protected boolean storeRelativePaths() {
				return config.isStoreRelativePaths();
			}
		};

		Util.setCenteredBounds(shell);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
	@NotNull private SimpleTableViewer<PatternAction> tableViewer;
	private final RegexTestPanel regexTestPanel;
	
	public PatternTable(@NotNull Composite parent,
						@NotNull IndexingConfig config) {
		// TODO i18n
		super(parent, SWT.NONE);
		setLayout(Util.createGridLayout(2, false, 0, 5));
		
		Table table = createTable();
		Control buttonPanel = createButtonPanel();
		
		regexTestPanel = new RegexTestPanel(this, config) {
			protected boolean storeRelativePaths() {
				return PatternTable.this.storeRelativePaths();
			}
		};
		
		GridData tableGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		int factor = ProgramConf.Int.PatternTableHeight.get() + 1; // +1 for column header
		tableGridData.minimumHeight = table.getItemHeight() * factor + 5;
		table.setLayoutData(tableGridData);
		
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 2));
		regexTestPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}
	
	@NotNull
	private Table createTable() {
		/*
		 * Note: The table has SWT.SINGLE style because moving more than one
		 * element up or down at once is currently not supported.
		 */
		tableViewer = new SimpleTableViewer<PatternAction>(this, SWT.BORDER | SWT.SINGLE);
		tableViewer.enableEditSupport();
		
		tableViewer.addColumn(new Column<PatternAction>("Pattern (regex)") {
			protected String getLabel(PatternAction element) {
				return element.getRegex();
			}
			protected ColumnEditSupport<PatternAction> getEditSupport() {
				return new TextEditSupport<PatternAction>() {
					protected void setText(PatternAction element, String text) {
						element.setRegex(text);
					}
				};
			}
		});
		
		tableViewer.addColumn(new Column<PatternAction>("Match Against") {
			protected String getLabel(PatternAction element) {
				switch (element.getTarget()) {
				case FILENAME: return "Filename";
				case FILEPATH: return "Path";
				}
				throw new IllegalStateException();
			}
			protected ColumnEditSupport<PatternAction> getEditSupport() {
				return new ComboEditSupport<PatternAction, MatchTarget>(MatchTarget.class) {
					protected void setChoice(	PatternAction element,
												MatchTarget target) {
						element.setTarget(target);
					}
					protected String toString(MatchTarget enumInstance) {
						return enumInstance.displayName;
					}
				};
			}
		});
		
		tableViewer.addColumn(new Column<PatternAction>("Action") {
			protected String getLabel(PatternAction element) {
				switch (element.getAction()) {
				case EXCLUDE: return "Exclude";
				case DETECT_MIME: return "Detect mime type (slower)";
				}
				throw new IllegalStateException();
			}
			protected ColumnEditSupport<PatternAction> getEditSupport() {
				return new ComboEditSupport<PatternAction, MatchAction>(MatchAction.class) {
					protected void setChoice(	PatternAction element,
												MatchAction action) {
						element.setAction(action);
					}
					protected String toString(MatchAction enumInstance) {
						return enumInstance.displayName;
					}
				};
			}
		});
		
		Table table = tableViewer.getControl();
		table.setLinesVisible(true);
		SettingsConf.ColumnWidths.PatternTable.bind(table);
		
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				regexTestPanel.setPatternActions(tableViewer.getSelection());
			}
		});
		
		return table;
	}

	@NotNull
	private Control createButtonPanel() {
		Composite comp = new Composite(this, SWT.NONE);
		comp.setLayout(Util.createGridLayout(1, false, 0, 5));
		
		Util.createPushButton(
			comp, Img.ADD.get(), "Add pattern", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tableViewer.add(new PatternAction());
				tableViewer.showElement(tableViewer.getItemCount() - 1);
				regexTestPanel.setPatternActions(tableViewer.getSelection());
			}
		});
		
		Util.createPushButton(
			comp, Img.REMOVE.get(), "Remove selected patterns",
			new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (PatternAction patternAction : tableViewer.getSelection())
					tableViewer.remove(patternAction);
				regexTestPanel.setPatternActions(tableViewer.getSelection());
			}
		});
		
		Util.createPushButton(
			comp, Img.ARROW_UP.get(), "Move Up", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				List<PatternAction> sel = tableViewer.getSelection();
				if (sel.size() == 1)
					tableViewer.move(sel.get(0), true);
			}
		});
		
		Util.createPushButton(
			comp, Img.ARROW_DOWN.get(), "Move Down", new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				List<PatternAction> sel = tableViewer.getSelection();
				if (sel.size() == 1)
					tableViewer.move(sel.get(0), false);
			}
		});
		
		return comp;
	}
	
	protected abstract boolean storeRelativePaths();
	
}
