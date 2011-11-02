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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.Main;
import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.gui.StatusBar.StatusBarPart;
import net.sourceforge.docfetcher.gui.filter.FileTypePanel;
import net.sourceforge.docfetcher.gui.filter.FilesizePanel;
import net.sourceforge.docfetcher.gui.filter.IndexPanel;
import net.sourceforge.docfetcher.gui.filter.ToolBarForm;
import net.sourceforge.docfetcher.gui.filter.TwoFormExpander;
import net.sourceforge.docfetcher.gui.pref.PrefDialog;
import net.sourceforge.docfetcher.gui.preview.PreviewPanel;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.FolderWatcher;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.model.parse.Parser;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.ConfLoader;
import net.sourceforge.docfetcher.util.ConfLoader.Loadable;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.collect.ListMap;
import net.sourceforge.docfetcher.util.gui.CocoaUIEnhancer;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;
import net.sourceforge.docfetcher.util.gui.LazyImageCache;
import net.sourceforge.docfetcher.util.gui.dialog.MultipleChoiceDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Joiner;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public final class Application {

	/** The widths of the sashes in pixels */
	private static final int sashWidth = 5;
	
	private static volatile IndexRegistry indexRegistry;
	private static volatile FolderWatcher folderWatcher;
	
	private static FilesizePanel filesizePanel;
	private static FileTypePanel fileTypePanel;
	private static IndexPanel indexPanel;

	private static Shell shell;
	private static ThreePanelForm threePanelForm;
	private static SearchBar searchBar;
	private static ResultPanel resultPanel;
	private static PreviewPanel previewPanel;
	private static StatusBarPart indexingStatus;
	private static SystemTrayHider systemTrayHider;

	private Application() {
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) {
		/*
		 * Load system constants; this should be the very first thing to do.
		 * We'll first try to load from the jar (normal use case), then from a
		 * file (we're inside the IDE).
		 */
		String systemConfName = "system.conf";
		String systemConfPath = "dev/system.conf";
		boolean success = ConfLoader.loadFromStreamOrFile(
			Main.class, SystemConf.class, systemConfName, systemConfPath);
		if (!success) {
			/*
			 * This is pretty bad, just give up here. Note that we cannot use
			 * the stacktrace utility methods at this point because we haven't
			 * loaded the constants in the utility class yet.
			 */
			Util.printErr("Couldn't find resource: " + systemConfName);
			System.exit(1);
		}

		// TODO now: See what happens if we don't initialize all constants
		// TODO i18n: Initialize MsgUtil and AppUtil.Msg, should be done as early as possible
		AppUtil.Const.PROGRAM_NAME.set(SystemConf.Str.ProgramName.get());
		AppUtil.Const.PROGRAM_VERSION.set(SystemConf.Str.ProgramVersion.get());
		AppUtil.Const.PROGRAM_BUILD_DATE.set(SystemConf.Str.BuildDate.get());
		AppUtil.Const.USER_DIR_PATH.set(Util.USER_DIR_PATH);
		AppUtil.Const.IS_PORTABLE.set(SystemConf.Bool.IsPortable.get());
		AppUtil.Const.IS_DEVELOPMENT_VERSION.set(SystemConf.Bool.IsDevelopmentVersion.get());

		if (!AppUtil.checkSingleInstance()) return;

		// Load program configuration and preferences
		loadProgramConf();
		File settingsConfFile = loadSettingsConf();
		
		// Determine shell title
		String shellTitle;
		if (SystemConf.Bool.IsDevelopmentVersion.get())
			shellTitle = SystemConf.Str.ProgramName.get();
		else
			shellTitle = ProgramConf.Str.AppName.get();
		
		// Load index registry; create display and shell
		Display.setAppName(shellTitle); // must be called *before* the display is created
		Display display = new Display();
		AppUtil.setDisplay(display);
		loadIndexRegistry(display);
		shell = new Shell(display);
		
		// Load images
		LazyImageCache lazyImageCache = new LazyImageCache(
			display, AppUtil.getImageDir());
		Img.initialize(lazyImageCache);
		lazyImageCache.reportMissingFiles(
			shell, Img.class, "Missing image files:");

		// Set shell icons, must be done *after* loading the images
		shell.setImages(new Image[] {
			Img.DOCFETCHER_16.get(), Img.DOCFETCHER_32.get(),
			Img.DOCFETCHER_48.get(), Img.DOCFETCHER_64.get(),
			Img.DOCFETCHER_128.get()});

		// Set default uncaught exception handler
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, final Throwable e) {
				AppUtil.showStackTrace(e);
			}
		});

		SettingsConf.ShellBounds.MainWindow.bind(shell);
		SettingsConf.Bool.MainShellMaximized.bindMaximized(shell);
		shell.setLayout(new FormLayout());
		shell.setText(shellTitle);
		
		initCocoaMenu(display);
		initSystemTrayHider();
		initThreePanelForm();
		StatusBar statusBar = initStatusBar();
		
		new SearchQueue(
			searchBar, filesizePanel, fileTypePanel, indexPanel, resultPanel);

		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.bottom().left().right().applyTo(statusBar);
		fdf.top().bottom(statusBar).applyTo(threePanelForm);

		// Move focus to search text field
		searchBar.setFocus();
		
		// Try to show the manual in the embedded browser
		boolean showManualHint = true;
		if (SettingsConf.Bool.ShowManualOnStartup.get()
				&& SettingsConf.Bool.ShowPreviewPanel.get()) {
			File file = ManualLocator.getManualFile();
			if (file == null) {
				showManualHint = false;
				AppUtil.showError("Manual not found!", true, true);
			}
			else if (previewPanel.setHtmlFile(file)) {
				showManualHint = false;
			}
		}
		if (showManualHint) {
			String msg = "Msg.press_help_button.format(Key.Help.toString())";
			statusBar.getLeftPart().setContents(Img.HELP.get(), msg);
		}
		
		// Global keyboard shortcuts
		display.addFilter(SWT.KeyUp, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				// TODO pre-release: global keys
			}
		});
		
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(final ShellEvent e) {
				handleShellClosed(e);
			}
		});
		
		// TODO pre-release: mark classes in gui package as final / package-visible when
		// possible -> move GUI classes above into gui package

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		/*
		 * Do not set this to null; the index registry loading thread must be
		 * able to see that the display was disposed.
		 */
		display.dispose();

		/*
		 * Try to save the settings. This may not be possible, for example when
		 * the user has burned the program onto a CD-ROM.
		 */
		if (settingsConfFile.canWrite()) {
			try {
				String comment = SettingsConf.loadHeaderComment();
				ConfLoader.save(settingsConfFile, SettingsConf.class, comment);
			}
			catch (IOException e) {
				AppUtil.showStackTraceInOwnDisplay(e);
			}
		}
	}

	private static void loadIndexRegistry(@NotNull final Display display) {
		File indexParentDir;
		if (SystemConf.Bool.IsDevelopmentVersion.get())
			indexParentDir = new File("bin/indexes");
		else if (SystemConf.Bool.IsPortable.get())
			indexParentDir = new File("indexes");
		else
			indexParentDir = AppUtil.getAppDataDir();
		
		// TODO now: make cache capacity customizable
		int reporterCapacity = ProgramConf.Int.MaxLinesInProgressPanel.get();
		indexRegistry = new IndexRegistry(indexParentDir, 20, reporterCapacity);
		
		new Thread(Application.class.getName() + " (load index registry)") {
			public void run() {
				try {
					indexRegistry.load(new Cancelable() {
						public boolean isCanceled() {
							return display.isDisposed();
						}
					});
					
					// Program may have been shut down while it was loading the indexes
					if (display.isDisposed())
						return;
					
					/*
					 * Install folder watches on the user's document folders.
					 * 
					 * This should be done *after* the index registry is loaded:
					 * The index registry will try to install its own folder
					 * watch during loading, and if we set up this folder
					 * watcher before loading the registry, we might take up all
					 * the allowed watches, so that there's none left for the
					 * registry.
					 */
					folderWatcher = new FolderWatcher(indexRegistry);
					
					/*
					 * Remove indexing hint from the status bar when the task
					 * queue has been emptied. This covers those situations
					 * where the indexing dialog has been minimized to the
					 * status bar and the last task in the queue has just been
					 * completed.
					 */
					indexRegistry.getQueue().evtQueueEmpty.add(new Event.Listener<Void>() {
						public void update(Void eventData) {
							Util.runAsyncExec(indexingStatus.getControl(), new Runnable() {
								public void run() {
									indexingStatus.setVisible(false);
								}
							});
						}
					});
				}
				catch (IOException e) {
					if (display.isDisposed())
						AppUtil.showStackTraceInOwnDisplay(e);
					else
						AppUtil.showStackTrace(e);
				}
			}
		}.start();
	}

	private static void loadProgramConf() {
		AppUtil.checkConstInitialized();
		AppUtil.ensureNoDisplay();
		
		File confFile;
		if (SystemConf.Bool.IsDevelopmentVersion.get()) {
			confFile = new File("dist/program.conf");
		}
		else {
			File appDataDir = AppUtil.getAppDataDir();
			confFile = new File(appDataDir, "conf/program.conf");
		}
		
		try {
			List<Loadable> notLoaded = ConfLoader.load(
				confFile, ProgramConf.class, false);
			if (!notLoaded.isEmpty()) {
				List<String> entryNames = new ArrayList<String>(
					notLoaded.size());
				for (Loadable entry : notLoaded)
					entryNames.add("  " + entry.name());
				String msg = String.format(
					"The following entries in '%s' "
							+ "are missing or have invalid values:\n",
					confFile.getName());
				msg += Joiner.on("\n").join(entryNames);
				AppUtil.showErrorOnStart(msg, false);
			}
		}
		catch (FileNotFoundException e) {
			/*
			 * Restore conf file if missing. In case of the non-portable
			 * version, the conf file will be missing the first time the program
			 * is started.
			 */
			InputStream in = Main.class.getResourceAsStream(confFile.getName());
			try {
				ConfLoader.load(in, ProgramConf.class);
				URL url = Resources.getResource(Main.class, confFile.getName());
				Util.getParentFile(confFile).mkdirs();
				Files.copy(
					Resources.newInputStreamSupplier(url), confFile);
			}
			catch (Exception e1) {
				AppUtil.showStackTraceInOwnDisplay(e1);
			}
			finally {
				Closeables.closeQuietly(in);
			}
		}
		catch (IOException e) {
			AppUtil.showStackTraceInOwnDisplay(e);
		}
	}

	private static File loadSettingsConf() {
		AppUtil.checkConstInitialized();
		AppUtil.ensureNoDisplay();
		
		File confFile;
		if (SystemConf.Bool.IsDevelopmentVersion.get()) {
			confFile = new File("bin/settings.conf");
		}
		else {
			File appDataDir = AppUtil.getAppDataDir();
			confFile = new File(appDataDir, "conf/settings.conf");
		}
		
		try {
			ConfLoader.load(confFile, SettingsConf.class, true);
		}
		catch (IOException e) {
			AppUtil.showStackTraceInOwnDisplay(e);
		}
		return confFile;
	}

	private static Control createLeftPanel(Composite parent) {
		final Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new FormLayout());

		ToolBarForm filesizeForm = new ToolBarForm(comp) {
			protected Control createToolBar(Composite parent) {
				final Label item = new Label(parent, SWT.NONE);
				item.setImage(Img.MINIMIZE.get());
				item.addMouseListener(new MouseAdapter() {
					public void mouseUp(MouseEvent e) {
						boolean isVisible = !isContentsVisible();
						setContentsVisible(isVisible);
						Image image = isVisible
							? Img.MINIMIZE.get()
							: Img.MAXIMIZE.get();
						item.setImage(image);
						comp.layout();
					}
				});
				Util.addMouseHighlighter(item);
				return item;
			}

			protected Control createContents(Composite parent) {
				filesizePanel = new FilesizePanel(parent);
				return filesizePanel.getControl();
			}
		};
		filesizeForm.setText("Minimum / Maximum Filesize");

		TwoFormExpander expander = new TwoFormExpander(comp) {
			protected Control createFirstContents(Composite parent) {
				// TODO websearch: Load parser states from file, save parser states to file?
				List<Parser> parsers = ParseService.getParsers();
				ListMap<Parser, Boolean> map = ListMap.create(parsers.size());
				for (Parser parser : parsers)
					map.add(parser, true);
				fileTypePanel = new FileTypePanel(parent, map);
				return fileTypePanel.getControl();
			}

			protected Control createSecondContents(Composite parent) {
				indexPanel = new IndexPanel(parent, indexRegistry);
				indexPanel.evtIndexingDialogMinimized.add(new Event.Listener<Rectangle>() {
					public void update(Rectangle eventData) {
						moveIndexingDialogToStatusBar(eventData);
					}
				});
				return indexPanel.getControl();
			}
		};
		expander.setTopText("Document Types");
		expander.setBottomText("Search Scope");
		expander.setSashWidth(sashWidth);

		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).left().top().right().applyTo(filesizeForm);
		fdf.top(filesizeForm, 5).bottom().applyTo(expander);

		return comp;
	}

	private static Control createRightTopPanel(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		searchBar = new SearchBar(comp);
		resultPanel = new ResultPanel(comp);

		comp.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).top().left().right().applyTo(searchBar.getControl());
		fdf.top(searchBar.getControl()).bottom().applyTo(resultPanel.getControl());
		
		searchBar.evtHideInSystemTray.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				systemTrayHider.hide();
			}
		});
		
		searchBar.evtOpenManual.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				File file = ManualLocator.getManualFile();
				if (file != null) {
					if (previewPanel.setHtmlFile(file))
						threePanelForm.setSecondSubControlVisible(true);
					else
						Util.launch(file);
				}
				else {
					AppUtil.showError("Manual not found!", true, true);
				}
			}
		});

		resultPanel.evtSelection.add(new Event.Listener<List<ResultDocument>>() {
			public void update(List<ResultDocument> eventData) {
				if (eventData.isEmpty()) return;
				previewPanel.setPreview(eventData.get(0));
				// TODO now: update status bar
			}
		});
		
		resultPanel.evtHideInSystemTray.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				systemTrayHider.hide();
			}
		});

		return comp;
	}
	
	private static void moveIndexingDialogToStatusBar(@NotNull Rectangle src) {
		indexingStatus.setVisible(true);
		Rectangle dest = indexingStatus.getBounds();
		dest = shell.getDisplay().map(shell, null, dest);
		MovingBox movingBox = new MovingBox(shell, src, dest, 0.2, 40);
		movingBox.start();
	}
	
	/*
	 * Sets up system tray hiding.
	 */
	private static void initSystemTrayHider() {
		systemTrayHider = new SystemTrayHider(shell);
		
		final ResultDocument[] lastDoc = new ResultDocument[1];
		
		systemTrayHider.evtHiding.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				/*
				 * If DocFetcher is sent to the system tray while being
				 * maximized and showing a big file on the preview panel, one
				 * would experience an annoying delay once the program returns
				 * from the system tray. The workaround is to clear the preview
				 * panel before going to the system tray and reset it when we
				 * come back.
				 */
				lastDoc[0] = previewPanel.clear();
			}
		});
		
		systemTrayHider.evtRestored.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				if (lastDoc[0] != null) {
					previewPanel.setPreview(lastDoc[0]);
					lastDoc[0] = null;
				}
				searchBar.setFocus();
			}
		});
		
		systemTrayHider.evtShutdown.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				shell.close();
			}
		});
	}

	private static void initCocoaMenu(@NotNull Display display) {
		if (!Util.IS_MAC_OS_X)
			return;
		
		CocoaUIEnhancer cocoaUIEnhancer = new CocoaUIEnhancer(ProgramConf.Str.AppName.get());
		cocoaUIEnhancer.hookApplicationMenu(display, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
//				shell.close(); // Not necessary
			}
		}, new Runnable() {
			public void run() {
				// TODO post-release-1.1: Show an about dialog? Or maybe open a manual page?
				AppUtil.showInfo("Dummy Message");
			}
		}, new Runnable() {
			public void run() {
				PrefDialog prefDialog = new PrefDialog(shell);
				prefDialog.open();
			}
		});
	}
	
	@NotNull
	private static void initThreePanelForm() {
		int filterPanelWidth = SettingsConf.Int.FilterPanelWidth.get();
		threePanelForm = new ThreePanelForm(shell, filterPanelWidth) {
			protected Control createFirstControl(Composite parent) {
				return createLeftPanel(parent);
			}
			protected Control createFirstSubControl(Composite parent) {
				return createRightTopPanel(parent);
			}
			protected Control createSecondSubControl(Composite parent) {
				return previewPanel = new PreviewPanel(parent);
			}
		};

		threePanelForm.setSashWidth(sashWidth);
		threePanelForm.setSubSashWidth(sashWidth);
		
		// Restore visibility of filter panel and preview panel
		threePanelForm.setFirstControlVisible(SettingsConf.Bool.ShowFilterPanel.get());
		threePanelForm.setSecondSubControlVisible(SettingsConf.Bool.ShowPreviewPanel.get());
		
		// Restore orientation and weights of right sash
		boolean isVertical = SettingsConf.Bool.ShowPreviewPanelAtBottom.get();
		threePanelForm.setVertical(isVertical);
		threePanelForm.setSubSashWeights(getRightSashWeights(isVertical));
		
		// Store visibility of filter panel
		threePanelForm.evtFirstControlShown.add(new Event.Listener<Boolean>() {
			public void update(Boolean eventData) {
				SettingsConf.Bool.ShowFilterPanel.set(eventData);
			}
		});
		
		// Store width of filter panel
		final Control leftControl = threePanelForm.getFirstControl();
		leftControl.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (leftControl.isVisible()) {
					int width = leftControl.getSize().x;
					SettingsConf.Int.FilterPanelWidth.set(width);
				}
			}
		});
		
		final boolean[] ignoreControlResize = { false };
		
		// Store weights of right sash
		Control topRightControl = threePanelForm.getFirstSubControl();
		ControlListener rightControlListener = new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (!previewPanel.isVisible() || ignoreControlResize[0])
					return;
				int[] weights = threePanelForm.getSubSashWeights();
				if (threePanelForm.isVertical())
					SettingsConf.IntArray.RightSashVertical.set(weights);
				else
					SettingsConf.IntArray.RightSashHorizontal.set(weights);
			}
		};
		topRightControl.addControlListener(rightControlListener);
		previewPanel.addControlListener(rightControlListener);
		
		// Store visibility of preview panel
		threePanelForm.evtSecondSubControlShown.add(new Event.Listener<Boolean>() {
			public void update(Boolean eventData) {
				SettingsConf.Bool.ShowPreviewPanel.set(eventData);
			}
		});
		
		/*
		 * Temporarily deactivate storing the sash weights during sash
		 * orientation changes. Without this, we'd set the same sash weights for
		 * both orientations.
		 */
		threePanelForm.evtSubOrientationChanging.add(new Event.Listener<Boolean>() {
			public void update(Boolean isVertical) {
				ignoreControlResize[0] = true;
			}
		});
		
		// Store orientation of right sash; update weights
		threePanelForm.evtSubOrientationChanged.add(new Event.Listener<Boolean>() {
			public void update(Boolean isVertical) {
				SettingsConf.Bool.ShowPreviewPanelAtBottom.set(isVertical);
				threePanelForm.setSubSashWeights(getRightSashWeights(isVertical));
				ignoreControlResize[0] = false;
			}
		});
	}
	
	@NotNull
	private static int[] getRightSashWeights(boolean isVertical) {
		return isVertical
			? SettingsConf.IntArray.RightSashVertical.get()
			: SettingsConf.IntArray.RightSashHorizontal.get();
	}
	
	@NotNull
	private static StatusBar initStatusBar() {
		return new StatusBar(shell) {
			public List<StatusBarPart> createRightParts(StatusBar statusBar) {
				indexingStatus = new StatusBarPart(statusBar, true);
				indexingStatus.setContents(Img.INDEXING.get(), "Indexing...");
				indexingStatus.setVisible(false);
				
				indexPanel.evtIndexingDialogOpened.add(new Event.Listener<Void>() {
					public void update(Void eventData) {
						indexingStatus.setVisible(false);
					}
				});
				
				indexingStatus.evtClicked.add(new Event.Listener<Void>() {
					public void update(Void eventData) {
						indexPanel.openIndexingDialog();
					}
				});
				
				StatusBarPart webInterfaceStatus = new StatusBarPart(statusBar, true);
				webInterfaceStatus.setContents(Img.INDEXING.get(), "Web Interface");

				List<StatusBarPart> parts = new ArrayList<StatusBarPart>(2);
				parts.add(indexingStatus);
				parts.add(webInterfaceStatus);
				return parts;
			}
		};
	}
	
	private static void handleShellClosed(@NotNull ShellEvent e) {
		e.doit = indexRegistry.getQueue().shutdown(new CancelHandler() {
			public CancelAction cancel() {
				return confirmExit();
			}
		});
		if (!e.doit)
			return;
		
		// Clear search history
		if (SettingsConf.Bool.ClearSearchHistoryOnExit.get())
			SettingsConf.StrList.SearchHistory.set();
		
		/*
		 * Note: The getSearcher() call below will block until the
		 * searcher is available. If we run this inside the GUI
		 * thread, we won't let go of the GUI lock, causing the
		 * program to deadlock when the user tries to close the
		 * program before all indexes have been loaded.
		 */
		new Thread() {
			public void run() {
				/*
				 * The folder watcher will be null if the program is
				 * shut down while loading the indexes
				 */
				if (folderWatcher != null)
					folderWatcher.shutdown();
				
				indexRegistry.getSearcher().shutdown();
			}
		}.start();
	}
	
	@Nullable
	private static CancelAction confirmExit() {
		MultipleChoiceDialog<CancelAction> dialog = new MultipleChoiceDialog<CancelAction>(shell);
		dialog.setTitle("Abort Indexing?");
		dialog.setText("An indexing process is still running and must be cancelled before terminating the program." +
				" Do you want to keep the index created so far? " +
				"Keeping it allows you to continue indexing later by running an index update.");
		
		dialog.addButton("&Keep", CancelAction.KEEP);
		dialog.addButton("&Discard", CancelAction.DISCARD);
		dialog.addButton("Don't &Exit", null);
		return dialog.open();
	}

}
