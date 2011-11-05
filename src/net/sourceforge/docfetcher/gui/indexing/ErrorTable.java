/*******************************************************************************
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
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.MultiFileLauncher;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.gui.ContextMenuManager;
import net.sourceforge.docfetcher.util.gui.MenuAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
final class ErrorTable {
	
	private final Table table;
	
	public ErrorTable(@NotNull Composite parent) {
		int style = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER;
		table = new Table(parent, style);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		// TODO i18n: column titles, context menu entries
		
		TableColumn errorTypeCol = new TableColumn(table, SWT.LEFT);
		TableColumn pathCol = new TableColumn(table, SWT.LEFT);
		errorTypeCol.setText("error_type");
		pathCol.setText("property_path");
		SettingsConf.ColumnWidths.IndexingErrorTable.bind(table);
		
		/*
		 * Open file associated with error item on doubleclick or when spacebar
		 * is pressed.
		 */
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				TableItem item = (TableItem) e.item;
				launchSelection(item);
			}
		});
		
		// Some keyboard shortcuts
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.stateMask == SWT.MOD1) {
					switch (e.keyCode) {
					case 'a': table.selectAll(); break;
					case 'c': copySelectedErrorsToClipboard(); break;
					}
				}
			}
		});
		
		initContextMenu();
	}
	
	@ThreadSafe
	public void addError(@NotNull final IndexingError error) {
		Util.checkNotNull(error);
		Util.runAsyncExec(table, new Runnable() {
			public void run() {
				TableItem item = new TableItem(table, SWT.NONE);
				String errorType = error.getErrorType().name();
				String filePath = error.getTreeNode().getPath().getCanonicalPath();
				item.setText(new String[] {errorType, filePath});
			}
		});
	}

	private void initContextMenu() {
		ContextMenuManager menuManager = new ContextMenuManager(table);
		
		menuManager.add(new MenuAction("open") {
			public boolean isEnabled() {
				return table.getSelectionCount() > 0;
			}
			public void run() {
				TableItem[] selection = table.getSelection();
				launchSelection(selection);
			}
			public boolean isDefaultItem() {
				// TODO now: windows: Does this work on Windows?
				return true;
			}
		});
		
		menuManager.add(new MenuAction("open_parent") {
			public boolean isEnabled() {
				return table.getSelectionCount() > 0;
			}
			public void run() {
				MultiFileLauncher launcher = new MultiFileLauncher();
				for (TableItem item : table.getSelection()) {
					File file = Util.getParentFile(item.getText(1));
					if (file.exists())
						launcher.addFile(file);
				}
				launcher.launch();
			}
		});
		
		menuManager.addSeparator();
		
		menuManager.add(new MenuAction("copy\tCtrl+C") {
			public boolean isEnabled() {
				return table.getItemCount() > 0;
			}
			public void run() {
				copySelectedErrorsToClipboard();
			}
		});
	}
	
	/**
	 * Copies the errors selected in the error panel to the clipboard. If the
	 * selection is empty, all errors are copied instead, if there are any. If
	 * there are no errors, this method does nothing.
	 */
	private void copySelectedErrorsToClipboard() {
		if (table.getItemCount() == 0)
			return;
		
		TableItem[] items = table.getSelection();
		if (items.length == 0)
			items = table.getItems();
		
		List<File> files = new ArrayList<File>(items.length);
		for (int i = 0; i < items.length; i++)
			files.add(new File(items[i].getText(1)));
		
		Util.setClipboard(files);
	}

	private void launchSelection(@NotNull TableItem... selection) {
		MultiFileLauncher launcher = new MultiFileLauncher();
		for (TableItem item : selection) {
			// Ignore non-existent files - these could be archive entries or
			// Outlook PST entries
			File file = new File(item.getText(1));
			if (file.exists())
				launcher.addFile(file);
		}
		launcher.launch();
	}

}
