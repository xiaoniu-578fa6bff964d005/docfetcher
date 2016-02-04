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
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.MultiFileLauncher;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.collect.AlphanumComparator;
import net.sourceforge.docfetcher.util.gui.ContextMenuManager;
import net.sourceforge.docfetcher.util.gui.MenuAction;
import net.sourceforge.docfetcher.util.gui.viewer.VirtualTableViewer;
import net.sourceforge.docfetcher.util.gui.viewer.VirtualTableViewer.Column;

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
	
	public final Event<Integer> evtErrorCountChanged = new Event<Integer>();
	
	private final VirtualTableViewer<IndexingError> tv;
	private final List<IndexingError> errors = new LinkedList<IndexingError>();
	
	public ErrorTable(@NotNull Composite parent) {
		int style = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER;
		
		tv = new VirtualTableViewer<IndexingError>(parent, style) {
			@SuppressWarnings("unchecked")
			protected List<IndexingError> getElements(Object rootElement) {
				return (List<IndexingError>) rootElement;
			}
		};
		
		tv.setSortingEnabled(true);
		tv.getControl().setLinesVisible(true);
		
		tv.addColumn(new Column<IndexingError>(Msg.document.get()) {
			protected String getLabel(IndexingError element) {
				return element.getTreeNode().getDisplayName();
			}
			protected int compare(IndexingError e1, IndexingError e2) {
				String l1 = getLabel(e1);
				String l2 = getLabel(e2);
				return AlphanumComparator.ignoreCaseInstance.compare(l1, l2);
			}
		});
		
		tv.addColumn(new Column<IndexingError>(Msg.error_message.get()) {
			protected String getLabel(IndexingError element) {
				return element.getLocalizedMessage();
			}
			protected int compare(IndexingError e1, IndexingError e2) {
				String l1 = getLabel(e1);
				String l2 = getLabel(e2);
				return AlphanumComparator.ignoreCaseInstance.compare(l1, l2);
			}
		});
		
		tv.addColumn(new Column<IndexingError>(Msg.path.get()) {
			protected String getLabel(IndexingError element) {
				try {
					return element.getTreeNode().getPath().getCanonicalPath();
				} catch (NullPointerException e) {
					/* Not fixing this due to lack of time :-/ */
					return element.getTreeNode().getName();
				}
			}
			protected int compare(IndexingError e1, IndexingError e2) {
				File f1 = e1.getTreeNode().getPath().getCanonicalFile();
				File f2 = e2.getTreeNode().getPath().getCanonicalFile();
				return f1.compareTo(f2);
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
				errors.add(error);
				tv.setRoot(errors);
				tv.scrollToBottom();
				evtErrorCountChanged.fire(errors.size());
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
					File file = null;
					try {
						file = error.getTreeNode().getPath().getCanonicalFile();
					} catch (NullPointerException e) {
						/*
						 * User comment in bug #1128: "I was 'opening parent
						 * folder' of files that could not be read. There were
						 * multiple files in one folder and I had already
						 * deleted them when I gave the command, thus the error,
						 * the files were gone."
						 */
						continue;
					}
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
				return !errors.isEmpty();
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
		if (errors.isEmpty())
			return;
		
		List<IndexingError> sel = tv.getSelection();
		if (sel.isEmpty())
			sel = errors;

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (IndexingError error : sel) {
			File file = error.getTreeNode().getPath().getCanonicalFile();
			if (!first)
				sb.append(Util.LS).append(Util.LS);
			sb.append(error.getLocalizedMessage());
			sb.append(Util.LS);
			sb.append(file.getPath());
			first = false;
		}
		
		Util.setClipboard(sb.toString());
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
