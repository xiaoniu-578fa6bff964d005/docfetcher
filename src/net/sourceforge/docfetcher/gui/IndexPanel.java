/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.gui.ContextMenuManager;
import net.sourceforge.docfetcher.base.gui.InputLoop;
import net.sourceforge.docfetcher.base.gui.MenuAction;
import net.sourceforge.docfetcher.base.gui.SimpleTreeViewer;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.indexing.IndexingDialog;
import net.sourceforge.docfetcher.gui.indexing.SingletonDialogFactory;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.IndexRegistry.AddedEvent;
import net.sourceforge.docfetcher.model.IndexRegistry.ExistingIndexesHandler;
import net.sourceforge.docfetcher.model.IndexRegistry.RemovedEvent;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.ViewNode;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingQueue;
import net.sourceforge.docfetcher.model.index.IndexingQueue.Rejection;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.index.file.FileIndex;
import net.sourceforge.docfetcher.model.index.outlook.OutlookIndex;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import com.sun.jna.platform.win32.Shell32Util;

public final class IndexPanel {
	
	public static abstract class DialogFactory extends SingletonDialogFactory<IndexingDialog> {
		public DialogFactory(Shell shell) {
			super(shell);
		}
	}
	
	public final Event<Void> evtCheckStatesChanged = new Event<Void>();

	private final SimpleTreeViewer<ViewNode> viewer;
	private final Tree tree;
	private final IndexRegistry indexRegistry;
	private final DialogFactory dialogFactory;

	public IndexPanel(	@NotNull final Composite parent,
						@NotNull final IndexRegistry indexRegistry) {
		Util.checkNotNull(parent, indexRegistry);
		this.indexRegistry = indexRegistry;

		dialogFactory = new DialogFactory(
			parent.getShell()) {
			protected IndexingDialog createDialog(Shell shell) {
				return new IndexingDialog(shell, indexRegistry);
			}
		};

		// TODO test code
		viewer = new SimpleTreeViewer<ViewNode>(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI) {
			protected Iterable<ViewNode> getChildren(ViewNode element) {
				return element.getChildren();
			};

			protected String getLabel(ViewNode element) {
				return element.getDisplayName();
			}
			
			protected boolean isChecked(ViewNode element) {
				return element.isChecked();
			}
			
			protected void setChecked(ViewNode element, boolean checked) {
				element.setChecked(checked);
				evtCheckStatesChanged.fire(null);
			}
		};
		tree = viewer.getControl();

		indexRegistry.addListeners(new ExistingIndexesHandler() {
			public void handleExistingIndexes(final List<LuceneIndex> indexes) {
				viewer.setRoots(UtilGlobal.<ViewNode> convert(indexes));
			}
		}, new Event.Listener<AddedEvent>() {
			public void update(final AddedEvent eventData) {
				Util.runSWTSafe(tree, new Runnable() {
					public void run() {
						viewer.addRootItem(eventData.added, eventData.newPos);
					}
				});
			}
		}, new Event.Listener<RemovedEvent>() {
			public void update(final RemovedEvent eventData) {
				Util.runSWTSafe(tree, new Runnable() {
					public void run() {
						viewer.remove(UtilGlobal.<ViewNode> convert(eventData.removed));
					}
				});
			}
		});

		initContextMenu();
	}

	private void initContextMenu() {
		ContextMenuManager menuManager = new ContextMenuManager(tree);
		// TODO set enabled states of menu items
		// TODO set keyboard shortcuts of menu items
		// TODO i18n menu item labels
		menuManager.add(new MenuAction("Create Index...") {
			public void run() {
				createFileTaskFromDialog(
					tree.getShell(), indexRegistry, dialogFactory);
			}
		});
		
		menuManager.addSeparator();
		
		class UpdateOrRebuildAction extends MenuAction {
			private final boolean isUpdate;
			public UpdateOrRebuildAction(String label, boolean isUpdate) {
				super(label);
				this.isUpdate = isUpdate;
			}
			public boolean isEnabled() {
				return isOnlyIndexesSelected();
			}
			public void run() {
				IndexingQueue queue = indexRegistry.getQueue();
				IndexAction action = isUpdate
					? IndexAction.UPDATE
					: IndexAction.REBUILD;
				for (LuceneIndex index : getSelectedIndexes())
					queue.addTask(index, action);
				dialogFactory.open();
			}
		}
		menuManager.add(new UpdateOrRebuildAction("Update Index...", true));
		menuManager.add(new UpdateOrRebuildAction("Rebuild Index...", false));
		
		menuManager.addSeparator();
		
		menuManager.add(new MenuAction("Delete Index...") {
			public boolean isEnabled() {
				return isOnlyIndexesSelected();
			}
			public void run() {
				// TODO deactivate index removal if a search is running?
				List<LuceneIndex> selectedIndexes = getSelectedIndexes();
				assert !selectedIndexes.isEmpty();
				// TODO i18n
				int ans = AppUtil.showConfirmation(
					"remove_orphaned_indexes_msg", false);
				if (ans == SWT.OK)
					indexRegistry.removeIndexes(selectedIndexes);
			}
		});
	}
	
	private boolean isOnlyIndexesSelected() {
		List<ViewNode> selection = viewer.getSelection();
		if (selection.isEmpty())
			return false;
		
		for (ViewNode viewNode : selection)
			if (!viewNode.isIndex())
				return false;
		return true;
	}
	
	@NotNull
	private List<LuceneIndex> getSelectedIndexes() {
		List<ViewNode> selection = viewer.getSelection();
		List<LuceneIndex> rootSelection = new ArrayList<LuceneIndex>(selection.size());
		for (ViewNode viewNode : selection)
			if (viewNode.isIndex())
				rootSelection.add((LuceneIndex) viewNode);
		return rootSelection;
	}

	@NotNull
	public Tree getControl() {
		return tree;
	}
	
	@NotNull
	public IndexRegistry getIndexRegistry() {
		return indexRegistry;
	}

	public static void createFileTaskFromDialog(@NotNull final Shell shell,
												@NotNull final IndexRegistry indexRegistry,
												@Nullable final DialogFactory dialogFactory) {
		String lastPath = SettingsConf.Str.LastIndexedFolder.get();
		if (!new File(lastPath).exists())
			lastPath = SettingsConf.Str.LastIndexedFolder.defaultValue;

		new InputLoop<Void>() {
			protected String getNewValue(String lastValue) {
				DirectoryDialog dialog = new DirectoryDialog(shell);
				dialog.setText("scope_folder_title"); // TODO i18n
				dialog.setMessage("scope_folder_msg"); // TODO i18n
				dialog.setFilterPath(lastValue);
				return dialog.open();
			}

			protected String getDenyMessage(String newValue) {
				IndexingConfig config = new IndexingConfig();
				File indexParentDir = indexRegistry.getIndexParentDir();
				File targetFile = new File(newValue);
				FileIndex index = new FileIndex(
					config, indexParentDir, targetFile);
				Rejection rejection = indexRegistry.getQueue().addTask(
					index, IndexAction.CREATE);
				if (rejection == null)
					return null;
				return "Rejected!"; // TODO i18n
			}

			protected Void onAccept(String newValue) {
				String absPath = Util.getSystemAbsPath(newValue);
				SettingsConf.Str.LastIndexedFolder.set(absPath); // TODO test
				if (dialogFactory != null)
					dialogFactory.open();
				return null;
			}
		}.run(lastPath);
	}

	public static void createOutlookTaskFromDialog(	@NotNull final Shell shell,
													@NotNull final IndexRegistry indexRegistry,
													@Nullable final DialogFactory dialogFactory) {
		String lastPath = SettingsConf.Str.LastIndexedPSTFile.get();
		if (!lastPath.equals("") && !new File(lastPath).isFile())
			lastPath = SettingsConf.Str.LastIndexedPSTFile.defaultValue;

		File pstFile = getOutlookPSTFile();
		if (pstFile != null) {
			// TODO On Windows, if the path is very long, the message dialog's width is too large
			String msg = "PST file found: " + pstFile + ". Index this file?";
			int ans = AppUtil.showConfirmation(msg, false); // TODO i18n
			if (ans == SWT.OK)
				lastPath = pstFile.getPath();
		}

		new InputLoop<Void>() {
			protected String getNewValue(String lastValue) {
				FileDialog dialog = new FileDialog(shell);
				// TODO i18n set dialog text and message
				dialog.setFilterExtensions(new String[] { "*.pst" });
				dialog.setFilterNames(new String[] { "Outlook Personal Storage Table (*.pst)" });
				if (!lastValue.equals(""))
					dialog.setFilterPath(lastValue);
				return dialog.open();
			}

			protected String getDenyMessage(String newValue) {
				IndexingConfig config = new IndexingConfig();
				File indexParentDir = indexRegistry.getIndexParentDir();
				File pstFile = new File(newValue);
				OutlookIndex index = new OutlookIndex(
					config, indexParentDir, pstFile);
				Rejection rejection = indexRegistry.getQueue().addTask(
					index, IndexAction.CREATE);
				if (rejection == null)
					return null;
				return "Rejected!"; // TODO i18n
			}

			protected Void onAccept(String newValue) {
				String path = Util.getSystemAbsPath(newValue);
				SettingsConf.Str.LastIndexedPSTFile.set(path); // TODO test
				if (dialogFactory != null)
					dialogFactory.open();
				return null;
			}
		}.run(lastPath);
	}

	/**
	 * Returns a file object representing the Outlook PST file of the Outlook
	 * instance installed on the system, or null if no such file is found. This
	 * method always returns null on non-Windows platforms.
	 */
	@Nullable
	private static File getOutlookPSTFile() {
		if (!Util.IS_WINDOWS)
			return null;
		/*
		 * Win7/Vista:
		 * C:\Users\<UserName>\AppData\Local\Microsoft\Outlook\Outlook.pst
		 * 
		 * WinXP/2000: C:\Documents and Settings\<UserName>\Local
		 * Settings\Application Data\Microsoft\Outlook\Outlook.pst
		 * 
		 * CSIDL_LOCAL_APPDATA = 0x001c
		 */
		String localAppData = Shell32Util.getFolderPath(0x001c);
		String pstFilePath = Util.joinPath(
			localAppData, "Microsoft/Outlook/Outlook.pst");
		File pstFile = new File(pstFilePath);
		return pstFile.isFile() ? pstFile : null;
	}

}
