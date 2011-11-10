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

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.MultiFileLauncher;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.gui.ContextMenuManager;
import net.sourceforge.docfetcher.util.gui.MenuAction;
import net.sourceforge.docfetcher.util.gui.viewer.SimpleTableViewer;
import net.sourceforge.docfetcher.util.gui.viewer.SimpleTableViewer.Column;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Tran Nam Quang
 */
final class ErrorTable {
	
	private final SimpleTableViewer<IndexingError> tv;
	
	public ErrorTable(@NotNull Composite parent) {
		int style = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER;
		tv = new SimpleTableViewer<IndexingError>(parent, style);
		tv.getControl().setLinesVisible(true);
		
		tv.addColumn(new Column<IndexingError>(Msg.document.get()) {
			protected String getLabel(IndexingError element) {
				return element.getTreeNode().getDisplayName();
			}
		});
		
		tv.addColumn(new Column<IndexingError>(Msg.error_message.get()) {
			protected String getLabel(IndexingError element) {
				return element.getLocalizedMessage();
			}
		});
		
		tv.addColumn(new Column<IndexingError>(Msg.path.get()) {
			protected String getLabel(IndexingError element) {
				return element.getTreeNode().getPath().getCanonicalPath();
			}
		});
		
		SettingsConf.ColumnWidths.IndexingErrorTable.bind(tv.getControl());
		
		/*
		 * Open file associated with error item on doubleclick or when spacebar
		 * is pressed.
		 */
		tv.getControl().addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				launchSelection();
			}
		});
		
		// Some keyboard shortcuts
		tv.getControl().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.stateMask == SWT.MOD1) {
					switch (e.keyCode) {
					case 'a': tv.getControl().selectAll(); break;
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
		Util.runAsyncExec(tv.getControl(), new Runnable() {
			public void run() {
				tv.add(error);
				tv.showElement(error);
			}
		});
	}

	private void initContextMenu() {
		ContextMenuManager menuManager = new ContextMenuManager(tv.getControl());
		
		menuManager.add(new MenuAction(Msg.open.get()) {
			public boolean isEnabled() {
				return !tv.getSelection().isEmpty();
			}
			public void run() {
				launchSelection();
			}
			public boolean isDefaultItem() {
				// TODO now: windows: Does this work on Windows?
				return true;
			}
		});
		
		menuManager.add(new MenuAction(Msg.open_parent.get()) {
			public boolean isEnabled() {
				return !tv.getSelection().isEmpty();
			}
			public void run() {
				MultiFileLauncher launcher = new MultiFileLauncher();
				for (IndexingError error : tv.getSelection()) {
					File file = error.getTreeNode().getPath().getCanonicalFile();
					File parent = Util.getParentFile(file);
					if (parent.exists())
						launcher.addFile(parent);
				}
				launcher.launch();
			}
		});
		
		menuManager.addSeparator();
		
		menuManager.add(new MenuAction(Msg.copy.get()) {
			public boolean isEnabled() {
				return tv.getItemCount() > 0;
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
		if (tv.getItemCount() == 0)
			return;
		
		List<IndexingError> errors = tv.getSelection();
		if (errors.isEmpty())
			tv.getElements();
		
		List<File> files = new ArrayList<File>(errors.size());
		for (IndexingError error : errors)
			files.add(error.getTreeNode().getPath().getCanonicalFile());
		
		Util.setClipboard(files);
	}

	private void launchSelection() {
		MultiFileLauncher launcher = new MultiFileLauncher();
		for (IndexingError error : tv.getSelection()) {
			// Ignore non-existent files - these could be archive entries or
			// Outlook PST entries
			File file = error.getTreeNode().getPath().getCanonicalFile();
			if (file.exists())
				launcher.addFile(file);
		}
		launcher.launch();
	}

}
