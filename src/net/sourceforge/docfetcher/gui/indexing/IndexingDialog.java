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
import java.util.List;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.gui.filter.IndexPanel;
import net.sourceforge.docfetcher.gui.indexing.KeepDiscardDialog.Answer;
import net.sourceforge.docfetcher.gui.indexing.SingletonDialogFactory.Dialog;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.index.DelegatingReporter.ExistingMessagesHandler;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.model.index.IndexingInfo;
import net.sourceforge.docfetcher.model.index.IndexingQueue.ExistingTasksHandler;
import net.sourceforge.docfetcher.model.index.Task;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.index.Task.TaskState;
import net.sourceforge.docfetcher.model.index.file.FileIndex;
import net.sourceforge.docfetcher.model.index.outlook.OutlookIndex;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.gui.DropDownMenuManager;
import net.sourceforge.docfetcher.util.gui.MenuAction;
import net.sourceforge.docfetcher.util.gui.TabFolderFactory;
import net.sourceforge.docfetcher.util.gui.ToolItemFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class IndexingDialog implements Dialog {
	
	/**
	 * This event is fired when the user clicks on the "Minimize To Status Bar"
	 * button. The event data is a rectangle describing the last bounds of the
	 * shell before it was disposed. The bounds are relative to the display.
	 */
	public final Event<Rectangle> evtDialogMinimized = new Event<Rectangle>();

	private final Shell shell;
	private final CTabFolder tabFolder;
	private final IndexRegistry indexRegistry;
	private boolean childDialogOpen = false;
	
	@NotNull private Event.Listener<Task> addedListener;
	@NotNull private Event.Listener<Task> removedListener;

	public IndexingDialog(	@NotNull final Shell parentShell,
							@NotNull final IndexRegistry indexRegistry) {
		Util.checkNotNull(parentShell, indexRegistry);
		this.indexRegistry = indexRegistry;

		// Create shell
		int style = SWT.SHELL_TRIM;
		if (!SystemConf.Bool.IsDevelopmentVersion.get()) // TODO pre-release: remove after testing
			style |= SWT.PRIMARY_MODAL;
		shell = new Shell(parentShell, style);
		shell.setText("index_management"); // TODO i18n
		shell.setImage(Img.INDEXING_DIALOG.get());
		shell.setLayout(Util.createFillLayout(5));
		SettingsConf.ShellBounds.IndexingDialog.bind(shell);
		
		// Create tabfolder
		boolean curvyTabs = ProgramConf.Bool.CurvyTabs.get();
		boolean coloredTabs = ProgramConf.Bool.ColoredTabs.get();
		tabFolder = TabFolderFactory.create(shell, true, curvyTabs, coloredTabs);
		
		// Create tabfolder toolbar
		ToolBar toolBar = new ToolBar(tabFolder, SWT.FLAT);
		tabFolder.setTopRight(toolBar);
		initToolBarMenu(toolBar);

		// For some unknown reason, the focus always goes to the ToolBar items
		toolBar.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				tabFolder.forceFocus();
			}
		});
		
		initEventHandlers();
	}

	@NotNull
	private void initToolBarMenu(@NotNull ToolBar toolBar) {
		ToolItemFactory tif = new ToolItemFactory(toolBar);
		
		// TODO i18n for all buttons
		
		final ToolItem addItem = tif.image(Img.ADD.get())
				.toolTip("add_to_queue").create();
		
		addItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				abstract class ChildDialogAction extends MenuAction {
					public ChildDialogAction(Image image, String label) {
						super(image, label);
					}
					public final void run() {
						// Only one child dialog can be open at any time
						assert !childDialogOpen;
						
						childDialogOpen = true;
						boolean success = doRun();
						childDialogOpen = false;
						
						/*
						 * If all existing tasks have been completed while the
						 * child dialog was open and the user cancelled the
						 * child dialog, close the indexing dialog.
						 */
						if (!success && tabFolder.getItemCount() == 0) {
							indexRegistry.getQueue().removeListeners(
								addedListener, removedListener);
							shell.dispose();
						}
					}
					protected abstract boolean doRun();
				}
				
				DropDownMenuManager menuManager = new DropDownMenuManager(
					addItem, tabFolder);
				
				menuManager.add(new ChildDialogAction(
					Img.FOLDER.get(), "Add Folder...") {
					public boolean doRun() {
						return IndexPanel.createFileTaskFromDialog(
							shell, indexRegistry, null, true);
					}
				});

				menuManager.addSeparator();

				menuManager.add(new ChildDialogAction(
					Img.PACKAGE.get(), "Add Archive...") {
					public boolean doRun() {
						return IndexPanel.createFileTaskFromDialog(
							shell, indexRegistry, null, false);
					}
				});

				menuManager.add(new ChildDialogAction(
					Img.EMAIL.get(), "Add Outlook PST...") {
					public boolean doRun() {
						return IndexPanel.createOutlookTaskFromDialog(
							shell, indexRegistry, null);
					}
				});
				
				menuManager.add(new MenuAction(
					Img.CLIPBOARD.get(), "Add From Clipboard...") {
					public void run() {
						IndexPanel.createTaskFromClipboard(
							shell, indexRegistry, null);
					}
				});
				
				menuManager.show();
			}
		});
		
		tif.image(Img.HIDE.get()).toolTip("Minimize To Status Bar")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						// TODO now: save all indexing config modifications
						indexRegistry.getQueue().removeListeners(
							addedListener, removedListener);
						Rectangle bounds = shell.getBounds();
						shell.dispose();
						evtDialogMinimized.fire(bounds);
					}
				}).create();
	}
	
	private void initEventHandlers() {
		addedListener = new Event.Listener<Task>() {
			public void update(final Task task) {
				assert !shell.isDisposed();
				Util.runSwtSafe(tabFolder, new Runnable() {
					public void run() {
						boolean isUpdate = task.is(IndexAction.UPDATE);
						boolean noTabs = tabFolder.getItemCount() == 0;
						addTab(task, !isUpdate || noTabs);
					}
				});
			}
		};
		
		removedListener = new Event.Listener<Task>() {
			public void update(final Task task) {
				assert !shell.isDisposed();
				Util.runSwtSafe(tabFolder, new Runnable() {
					public void run() {
						for (CTabItem item : tabFolder.getItems()) {
							if (item.getData() == task) {
								item.dispose();
								break;
							}
						}
						
						/*
						 * If there are no more tabs and no child dialogs are
						 * open, close the indexing dialog.
						 */
						if (!childDialogOpen && tabFolder.getItemCount() == 0) {
							indexRegistry.getQueue().removeListeners(
								addedListener, removedListener);
							shell.dispose();
						}
					}
				});
			}
		};
		
		/*
		 * Hook onto the indexing queue, i.e. register the listeners and create
		 * tabs for existing tasks as necessary.
		 */
		indexRegistry.getQueue().addListeners(new ExistingTasksHandler() {
			public void handleExistingTasks(List<Task> tasks) {
				boolean selectTab = tabFolder.getItemCount() == 0;
				for (Task task : tasks) {
					addTab(task, selectTab);
					selectTab = false;
				}
			}
		}, addedListener, removedListener);
		
		/*
		 * When the indexing dialog is closed, cancel all tasks and unregister
		 * the listeners.
		 */
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(final ShellEvent e) {
				indexRegistry.getQueue().removeAll(new CancelHandler() {
					public CancelAction cancel() {
						CancelAction action = confirmCancel();
						e.doit = action != null;
						return action;
					}
				}, addedListener, removedListener);
			}
		});
		
		// Handle closing of tabs by the user
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(final CTabFolderEvent event) {
				if (tabFolder.getItemCount() == 1) {
					/*
					 * Closing the last tab automatically triggers the closing
					 * of the shell. However, there can be a considerable delay
					 * between the former and the latter, allowing the user to
					 * click on the close button of the shell before the shell
					 * is closed automatically. If the user does that, there
					 * will be the strange effect of DocFetcher asking for
					 * keep/discard/cancel confirmation *twice*: First after
					 * clicking the close button of the tab, then after clicking
					 * the close button of the shell. To avoid this, when the
					 * user tries to close the last tab, we'll close the shell
					 * instead.
					 * 
					 * Note that event.doit must be set to false here: When the
					 * shell is about to be closed and the confirmation dialog
					 * shows up, the user could click on the 'Cancel' button, in
					 * which case the tab should remain open.
					 */
					event.doit = false;
					shell.close();
					return;
				}
				Task task = (Task) event.item.getData();
				task.remove(new CancelHandler() {
					public CancelAction cancel() {
						CancelAction action = confirmCancel();
						event.doit = action != null;
						return action;
					}
				});
			}
		});
	}
	
	@Nullable
	private CancelAction confirmCancel() {
		KeepDiscardDialog dialog = new KeepDiscardDialog(shell);
		Answer answer = dialog.open();
		switch (answer) {
		case KEEP:
			return CancelAction.KEEP;
		case DISCARD:
			return CancelAction.DISCARD;
		case CONTINUE:
			return null;
		}
		throw new IllegalStateException();
	}

	@NotNull
	public Shell getShell() {
		return shell;
	}

	private void addTab(@NotNull final Task task, boolean selectTab) {
		// Create an configure tab item
		final CTabItem tabItem = new CTabItem(tabFolder, SWT.CLOSE);
		tabItem.setData(task);
		LuceneIndex index = task.getLuceneIndex();
		File rootFile = index.getRootFile();
		String nameOrLetter = Util.getNameOrLetter(rootFile, ":\\");
		tabItem.setText(Util.truncate(nameOrLetter));
		tabItem.setToolTipText(Util.getSystemAbsPath(rootFile));
		if (task.is(TaskState.READY))
			tabItem.setImage(Img.TREE.get());
		else
			tabItem.setImage(Img.CHECK.get());
		
		/*
		 * The tab item's control will not be disposed when the tab item is
		 * disposed, so this dispose listener is necessary. Note that the
		 * control to be disposed might be either the configuration panel or the
		 * progress panel, so calling configPanel.dispose() is not correct.
		 */
		tabItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				tabItem.getControl().dispose();
			}
		});

		IndexingConfig config = index.getConfig();
		
		if (task.is(IndexAction.UPDATE) || !task.is(TaskState.NOT_READY)) {
			switchToProgressPanel(task, tabItem, config);
		}
		else {
			final ConfigPanel configPanel;
			if (index instanceof FileIndex)
				configPanel = new FileConfigPanel(tabFolder, config);
			else if (index instanceof OutlookIndex)
				configPanel = new OutlookConfigPanel(tabFolder, config);
			else
				throw new IllegalStateException();
			tabItem.setControl(configPanel.getControl());

			/*
			 * Move focus away from tab item, or else the tab title will be
			 * underlined.
			 */
			configPanel.getControl().setFocus();

			configPanel.evtRunButtonClicked.add(new Event.Listener<IndexingConfig>() {
				public void update(final IndexingConfig config) {
					tabItem.setImage(Img.TREE.get());
					configPanel.getControl().dispose();
					switchToProgressPanel(task, tabItem, config);
					task.setReady();
					
					/*
					 * Switch to next waiting tab. This is thread-safe
					 * even though it's a check-then-act operation
					 * because only the user can change the task state
					 * from 'not ready' to 'ready'.
					 */
					for (CTabItem candidateItem : tabFolder.getItems()) {
						Task candidateTask = (Task) candidateItem.getData();
						if (!candidateTask.is(TaskState.READY)) {
							tabFolder.setSelection(candidateItem);
							break;
						}
					}
				}
			});
		}

		if (selectTab)
			tabFolder.setSelection(tabItem);
	}

	private void switchToProgressPanel(	@NotNull final Task task,
										@NotNull final CTabItem tabItem,
										@NotNull final IndexingConfig config) {
		ProgressPanel progressPanel = new ProgressPanel(tabFolder);
		tabItem.setControl(progressPanel.getControl());
		final ProgressReporter reporter = new ProgressReporter(progressPanel);
		
		task.attachReporter(reporter, new ExistingMessagesHandler() {
			public void handleMessages(	List<IndexingInfo> infos,
										List<IndexingError> errors) {
				/*
				 * Dangerous section: This method runs under the lock of the
				 * DelegatingReporter instance, so beware of lock-ordering
				 * deadlocks.
				 */
				for (IndexingInfo info : infos)
					reporter.info(info);
				for (IndexingError error : errors)
					reporter.fail(error);
			}
		});
		
		/*
		 * For unknown reasons, without this line the progress table won't
		 * scroll to the bottom until the next message comes in.
		 */
		progressPanel.getProgressTable().scrollToBottom();
		
		progressPanel.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				task.detachReporter(reporter);
			}
		});
	}

}
