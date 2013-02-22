/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.Main;
import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.gui.StatusBar.StatusBarPart;
import net.sourceforge.docfetcher.gui.filter.FileTypePanel;
import net.sourceforge.docfetcher.gui.filter.FilesizePanel;
import net.sourceforge.docfetcher.gui.filter.IndexPanel;
import net.sourceforge.docfetcher.gui.filter.ToolBarForm;
import net.sourceforge.docfetcher.gui.filter.TwoFormExpander;
import net.sourceforge.docfetcher.gui.filter.TwoFormExpander.MaximizedControl;
import net.sourceforge.docfetcher.gui.pref.PrefDialog;
import net.sourceforge.docfetcher.gui.preview.PreviewPanel;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.Daemon;
import net.sourceforge.docfetcher.model.FolderWatcher;
import net.sourceforge.docfetcher.model.IndexLoadingProblems;
import net.sourceforge.docfetcher.model.IndexLoadingProblems.CorruptedIndex;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.index.IndexingQueue;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.model.parse.Parser;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.CharsetDetectorHelper;
import net.sourceforge.docfetcher.util.ConfLoader;
import net.sourceforge.docfetcher.util.ConfLoader.Loadable;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.collect.AlphanumComparator;
import net.sourceforge.docfetcher.util.collect.ListMap;
import net.sourceforge.docfetcher.util.gui.CocoaUIEnhancer;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;
import net.sourceforge.docfetcher.util.gui.LazyImageCache;
import net.sourceforge.docfetcher.util.gui.dialog.InfoDialog;
import net.sourceforge.docfetcher.util.gui.dialog.ListConfirmDialog;
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
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public final class Application {

	// TODO post-release-1.1: review visibility of all DocFetcher classes

	/** The widths of the sashes in pixels */
	private static final int sashWidth = 5;

	private static volatile IndexRegistry indexRegistry;
	private static volatile FolderWatcher folderWatcher;
	@Nullable private static HotkeyHandler hotkeyHandler;
	private static File programConfFile;

	private static FilesizePanel filesizePanel;
	private static FileTypePanel fileTypePanel;
	private static IndexPanel indexPanel;

	private static volatile Shell shell;
	private static ThreePanelForm threePanelForm;
	private static SearchBar searchBar;
	private static ResultPanel resultPanel;
	private static PreviewPanel previewPanel;
	private static volatile StatusBarPart indexingStatus;
	private static SystemTrayHider systemTrayHider;
	private static StatusBar statusBar;
	private static boolean systemTrayShutdown = false;
	private static File settingsConfFile;
	
	private static boolean indexRegistryLoaded = false; // should only be accessed from SWT thread
	private static Runnable clearIndexLoadingMsg; // should only be accessed from SWT thread

	private Application() {
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) {
		/*
		 * Bug #3553412: Starting with Java 7, calling Arrays.sort can cause an
		 * IllegalArgumentException with the error message
		 * "Comparison method violates its general contract!". In DocFetcher,
		 * this happened on a PDF file with PDFBox 1.7.0. For background, see
		 * http://stackoverflow.com/questions/7849539/comparison-method
		 * -violates-its-general-contract-java-7-only
		 */
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		
		/*
		 * Load system constants; this should be the very first thing to do.
		 * We'll first try to load from the jar (normal use case), then from a
		 * file (we're inside the IDE).
		 */
		String systemConfName = "system-conf.txt";
		String systemConfPath = "dev/system-conf.txt";
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

		AppUtil.Const.PROGRAM_NAME.set(SystemConf.Str.ProgramName.get());
		AppUtil.Const.PROGRAM_VERSION.set(SystemConf.Str.ProgramVersion.get());
		AppUtil.Const.PROGRAM_BUILD_DATE.set(SystemConf.Str.BuildDate.get());
		AppUtil.Const.USER_DIR_PATH.set(Util.USER_DIR_PATH);
		AppUtil.Const.IS_PORTABLE.set(SystemConf.Bool.IsPortable.get());
		AppUtil.Const.IS_DEVELOPMENT_VERSION.set(SystemConf.Bool.IsDevelopmentVersion.get());

		Msg.loadFromDisk();
		Msg.setCheckEnabled(false);
		AppUtil.Messages.system_error.set(Msg.system_error.get());
		AppUtil.Messages.confirm_operation.set(Msg.confirm_operation.get());
		AppUtil.Messages.invalid_operation.set(Msg.invalid_operation.get());
		AppUtil.Messages.program_died_stacktrace_written.set(Msg.report_bug.get());
		AppUtil.Messages.program_running_launch_another.set(Msg.program_running_launch_another.get());
		AppUtil.Messages.ok.set(Msg.ok.get());
		AppUtil.Messages.cancel.set(Msg.cancel.get());
		Msg.setCheckEnabled(true);
		AppUtil.Messages.checkInitialized();

		/*
		 * Set the path from which to load the native SWT libraries. This must
		 * be done before doing the single instance check, because that's where
		 * we're going to use SWT for the first time, by opening a message
		 * dialog.
		 *
		 * If we don't set the path, SWT will extract its native libraries into
		 * ${user.home}/.swt. This is unsuitable especially for the portable
		 * version of DocFetcher, which should not leave any files in the
		 * system.
		 * 
		 * Bug #399: The DLLs for 32-bit and 64-bit Windows have the same names,
		 * so we must extract them into separate directories in order to avoid
		 * name clashes, which would cause the program to fail during startup.
		 * https://sourceforge.net/p/docfetcher/bugs/399/
		 */
		String swtLibSuffix = AppUtil.isPortable() ? "lib/swt/" : "swt/";
		if (Util.IS_WINDOWS)
			swtLibSuffix += "windows-";
		else if (Util.IS_LINUX)
			swtLibSuffix += "linux-";
		else if (Util.IS_MAC_OS_X)
			swtLibSuffix += "macosx-";
		else
			swtLibSuffix += "unknown-";
		if (Util.IS_64_BIT_JVM)
			swtLibSuffix += "64";
		else
			swtLibSuffix += "32";
		File swtLibDir = new File(AppUtil.getAppDataDir(), swtLibSuffix);
		swtLibDir.mkdirs(); // SWT won't recognize the path if it doesn't exist
		System.setProperty("swt.library.path", Util.getAbsPath(swtLibDir));

		// Load program configuration and preferences
		File confPathOverride = null;
		File indexPathOverride = null;
		try {
			Properties pathProps = CharsetDetectorHelper.load(new File("misc", "paths.txt"));
			confPathOverride = toFile(pathProps, "settings");
			indexPathOverride = toFile(pathProps, "indexes");
		}
		catch (IOException e1) {
			// Ignore
		}
		programConfFile = loadProgramConf(confPathOverride);
		settingsConfFile = loadSettingsConf(confPathOverride);
		
		// Update indexes in headless mode
		if (args.length >= 1 && args[0].equals("--update-indexes")) {
			loadIndexRegistryHeadless(getIndexParentDir(indexPathOverride));
			return;
		}

		// Check single instance
		if (!AppUtil.checkSingleInstance())
			return;
		
		checkMultipleDocFetcherJars();

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
		shell = new Shell(display);
		loadIndexRegistry(shell, getIndexParentDir(indexPathOverride));

		// Load images
		LazyImageCache lazyImageCache = new LazyImageCache(
			display, AppUtil.getImageDir());
		Img.initialize(lazyImageCache);
		lazyImageCache.reportMissingFiles(
			shell, Img.class, Msg.missing_image_files.get());

		// Set shell icons, must be done *after* loading the images
		shell.setImages(new Image[] {
			Img.DOCFETCHER_16.get(), Img.DOCFETCHER_32.get(),
			Img.DOCFETCHER_48.get(), Img.DOCFETCHER_64.get(),
			Img.DOCFETCHER_128.get()});

		// Set default uncaught exception handler
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, final Throwable e) {
				handleCrash(e);
			}
		});

		SettingsConf.ShellBounds.MainWindow.bind(shell);
		SettingsConf.Bool.MainShellMaximized.bindMaximized(shell);
		shell.setLayout(new FormLayout());
		shell.setText(shellTitle);

		initCocoaMenu(display);
		initSystemTrayHider();
		initThreePanelForm();
		initStatusBar();
		initHotkey();
		initGlobalKeys(display);

		new SearchQueue(
			searchBar, filesizePanel, fileTypePanel, indexPanel, resultPanel,
			statusBar);

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
				String msg = Msg.file_not_found.get() + "\n" + ManualLocator.manualFilename;
				AppUtil.showError(msg, true, true);
			}
			else if (previewPanel.setHtmlFile(file)) {
				showManualHint = false;
			}
		}
		if (showManualHint) {
			String msg = Msg.press_f1_for_help.get();
			statusBar.getLeftPart().setContents(Img.HELP.get(), msg);
		}

		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(final ShellEvent e) {
				handleShellClosed(e);
			}
		});

		shell.open();
		while (!shell.isDisposed()) {
			try {
				if (!display.readAndDispatch())
					display.sleep();
			}
			catch (Throwable t) {
				handleCrash(t);
			}
		}

		/*
		 * Do not set this to null; the index registry loading thread must be
		 * able to see that the display was disposed.
		 */
		display.dispose();
		saveSettingsConfFile();
	}
	
	@Nullable
	private static File toFile(@NotNull Properties props, @NotNull String key) {
		String value = props.getProperty(key);
		if (value == null)
			return null;
		return Util.getCanonicalFile(value);
	}
	
	private static void handleCrash(@NotNull Throwable t) {
		for (OutOfMemoryError e : Iterables.filter(Throwables.getCausalChain(t), OutOfMemoryError.class)) {
			UtilGui.showOutOfMemoryMessage(shell, e);
			return;
		}
		AppUtil.showStackTrace(t);
	}

	private static void saveSettingsConfFile() {
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
	
	/**
	 * Checks for multiple loaded DocFetcher jars. This should be called before
	 * creating the display.
	 */
	private static void checkMultipleDocFetcherJars() {
		if (SystemConf.Bool.IsDevelopmentVersion.get())
			return;
		if (!(AppUtil.isPortable() || Util.IS_WINDOWS))
			return;
		Pattern p = Pattern.compile("net\\.sourceforge\\.docfetcher.*\\.jar");
		List<File> dfJars = new LinkedList<File>();
		for (File jarFile : Util.listFiles(new File("lib")))
			if (p.matcher(jarFile.getName()).matches())
				dfJars.add(jarFile);
		if (dfJars.size() == 1)
			return;
		assert !dfJars.isEmpty();
		String msg = Msg.multiple_docfetcher_jars.format(Util.join("\n", dfJars));
		AppUtil.showErrorOnStart(msg, false);
	}

	private static void initGlobalKeys(@NotNull Display display) {
		/*
		 * This filter must be added to SWT.KeyDown rather than SWT.KeyUp,
		 * otherwise we won't be able to prevent the events from propagating
		 * further.
		 */
		display.addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event e) {
				// Disable global keys when the main shell is inactive
				if (Display.getCurrent().getActiveShell() != shell)
					return;

				e.doit = false;
				int m = e.stateMask;
				int k = e.keyCode;

				if (k == SWT.F1) {
					showManual();

					// Clear "Press F1" help message from status bar
					String msg = Msg.press_f1_for_help.get();
					StatusBarPart statusBarPart = statusBar.getLeftPart();
					if (msg.equals(statusBarPart.getText()))
						statusBarPart.setContents(null, "");
				}
				else if ((m & (SWT.ALT | SWT.CTRL | SWT.COMMAND)) != 0 && k == 'f') {
					searchBar.setFocus();
				}
				else {
					e.doit = true;
				}
			}
		});
	}
	
	@NotNull
	private static File getIndexParentDir(@Nullable File pathOverride) {
		File indexParentDir;
		if (SystemConf.Bool.IsDevelopmentVersion.get()) {
			indexParentDir = new File("bin/indexes");
		}
		else if (pathOverride != null && !pathOverride.isFile()) {
			pathOverride.mkdirs();
			indexParentDir = pathOverride;
		}
		else {
			File appDataDir = AppUtil.getAppDataDir();
			if (SystemConf.Bool.IsPortable.get())
				indexParentDir = new File(appDataDir, "indexes");
			else
				indexParentDir = appDataDir;
		}
		indexParentDir.mkdirs();
		return indexParentDir;
	}

	private static void loadIndexRegistry(@NotNull final Shell mainShell, @NotNull File indexParentDir) {
		Util.assertSwtThread();
		final Display display = mainShell.getDisplay();

		int cacheCapacity = ProgramConf.Int.UnpackCacheCapacity.get();
		int reporterCapacity = ProgramConf.Int.MaxLinesInProgressPanel.get();
		indexRegistry = new IndexRegistry(
			indexParentDir, cacheCapacity, reporterCapacity);
		final Daemon daemon = new Daemon(indexRegistry);
		IndexingQueue queue = indexRegistry.getQueue();

		queue.evtWorkerThreadTerminated.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				daemon.writeIndexesToFile();
			}
		});

		/*
		 * Remove indexing hint from the status bar when the task
		 * queue has been emptied. This covers those situations
		 * where the indexing dialog has been minimized to the
		 * status bar and the last task in the queue has just been
		 * completed.
		 */
		queue.evtQueueEmpty.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				/*
				 * Bug #3485598: The indexing status widget can be null at this
				 * point. Possible explanation: An indexing update was issued by
				 * the DocFetcher daemon and finished before the GUI was fully
				 * initialized.
				 */
				if (indexingStatus == null)
					return;
				Util.runAsyncExec(indexingStatus.getControl(), new Runnable() {
					public void run() {
						indexingStatus.setVisible(false);
					}
				});
			}
		});

		new Thread(Application.class.getName() + " (load index registry)") {
			public void run() {
				try {
					final IndexLoadingProblems loadingProblems = indexRegistry.load(new Cancelable() {
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

					// Show error message when watch limit is reached
					folderWatcher.evtWatchLimitError.add(new Event.Listener<String>() {
						public void update(final String eventData) {
							Util.runAsyncExec(mainShell, new Runnable() {
								public void run() {
									InfoDialog dialog = new InfoDialog(mainShell);
									dialog.setTitle(Msg.system_error.get());
									dialog.setImage(SWT.ICON_ERROR);
									dialog.setText(eventData);
									dialog.open();
								}
							});
						}
					});

					// Must be called *after* the indexes have been loaded
					daemon.enqueueUpdateTasks();
					
					// Confirm deletion of obsolete files inside the index
					// folder
					if (ProgramConf.Bool.ReportObsoleteIndexFiles.get()
							&& !loadingProblems.getObsoleteFiles().isEmpty()) {
						Util.runSyncExec(mainShell, new Runnable() {
							public void run() {
								reportObsoleteIndexFiles(
									mainShell,
									indexRegistry.getIndexParentDir(),
									loadingProblems.getObsoleteFiles());
							}
						});
					}

					// Show error messages if some indexes couldn't be loaded
					if (!loadingProblems.getCorruptedIndexes().isEmpty()) {
						StringBuilder msg = new StringBuilder(Msg.corrupted_indexes.get());
						for (CorruptedIndex index : loadingProblems.getCorruptedIndexes()) {
							msg.append("\n\n");
							String indexName = index.index.getRootFolder().getDisplayName();
							String errorMsg = index.ioException.getMessage();
							msg.append(Msg.index.format(indexName));
							msg.append("\n");
							msg.append(Msg.error.format(errorMsg));
						}
						AppUtil.showError(msg.toString(), true, false);
					}
				}
				catch (IOException e) {
					if (display.isDisposed())
						AppUtil.showStackTraceInOwnDisplay(e);
					else
						AppUtil.showStackTrace(e);
				}
				finally {
					Util.runAsyncExec(mainShell, new Runnable() {
						public void run() {
							indexRegistryLoaded = true;
							if (clearIndexLoadingMsg != null)
								clearIndexLoadingMsg.run();
						}
					});
				}
			}
		}.start();
	}
	
	private static void loadIndexRegistryHeadless(@NotNull File indexParentDir) {
		int cacheCapacity = ProgramConf.Int.UnpackCacheCapacity.get();
		int reporterCapacity = ProgramConf.Int.MaxLinesInProgressPanel.get();
		indexRegistry = new IndexRegistry(
			indexParentDir, cacheCapacity, reporterCapacity);
		
		try {
			indexRegistry.load(Cancelable.nullCancelable);
			final IndexingQueue queue = indexRegistry.getQueue();
			
			queue.evtQueueEmpty.add(new Event.Listener<Void>() {
				public void update(Void eventData) {
					indexRegistry.getSearcher().shutdown();
					queue.shutdown(new CancelHandler() {
						public CancelAction cancel() {
							return CancelAction.KEEP;
						}
					});
				}
			});
			
			for (LuceneIndex index : indexRegistry.getIndexes())
				queue.addTask(index, IndexAction.UPDATE);
		}
		catch (IOException e) {
			Util.printErr(e);
		}
	}
	
	private static void reportObsoleteIndexFiles(	@NotNull Shell mainShell,
	                                             	@NotNull File indexDir,
													@NotNull List<File> filesToDelete) {
		ListConfirmDialog dialog = new ListConfirmDialog(mainShell, SWT.ICON_INFORMATION);
		dialog.setTitle(Msg.confirm_operation.get());
		dialog.setText(Msg.delete_obsolete_index_files.format(indexDir.getPath()));
		dialog.setButtonLabels(Msg.delete_bt.get(), Msg.keep.get());
		
		filesToDelete = new ArrayList<File>(filesToDelete);
		Collections.sort(filesToDelete, new Comparator<File>() {
			public int compare(File o1, File o2) {
				boolean d1 = o1.isDirectory();
				boolean d2 = o2.isDirectory();
				if (d1 && !d2)
					return -1;
				if (!d1 && d2)
					return 1;
				return AlphanumComparator.ignoreCaseInstance.compare(o1.getName(), o2.getName());
			}
		});
		
		for (File file : filesToDelete) {
			Image img = (file.isDirectory() ? Img.FOLDER : Img.FILE).get();
			dialog.addItem(img, file.getName());
		}
		
		dialog.evtLinkClicked.add(new Event.Listener<String>() {
			public void update(String eventData) {
				Util.launch(eventData);
			}
		});
		
		if (dialog.open()) {
			for (File file : filesToDelete) {
				try {
					Util.deleteRecursively(file);
				}
				catch (IOException e) {
					Util.printErr(e);
				}
			}
		}
	}

	private static File loadProgramConf(@Nullable File pathOverride) {
		AppUtil.checkConstInitialized();
		AppUtil.ensureNoDisplay();

		File confFile;
		if (SystemConf.Bool.IsDevelopmentVersion.get()) {
			confFile = new File("dist/program-conf.txt");
		}
		else if (pathOverride != null && !pathOverride.isFile()) {
			pathOverride.mkdirs();
			confFile = new File(pathOverride, "program-conf.txt");
		}
		else {
			File appDataDir = AppUtil.getAppDataDir();
			confFile = new File(appDataDir, "conf/program-conf.txt");
		}

		try {
			List<Loadable> notLoaded = ConfLoader.load(
				confFile, ProgramConf.class, false);
			if (!notLoaded.isEmpty()) {
				List<String> entryNames = new ArrayList<String>(
					notLoaded.size());
				for (Loadable entry : notLoaded)
					entryNames.add("  " + entry.name());
				String msg = Msg.entries_missing.format(confFile.getName());
				msg += "\n" + Joiner.on("\n").join(entryNames);
				msg += "\n\n" + Msg.entries_missing_regenerate.get();
				int style = SWT.YES | SWT.NO | SWT.ICON_WARNING;
				if (AppUtil.showErrorOnStart(msg, style) == SWT.YES) {
					regenerateConfFile(confFile);
				}
			}
		}
		catch (FileNotFoundException e) {
			regenerateConfFile(confFile);
		}
		catch (IOException e) {
			AppUtil.showStackTraceInOwnDisplay(e);
		}
		return confFile;
	}

	private static void regenerateConfFile(File confFile) {
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
	
	private static File loadSettingsConf(@Nullable File pathOverride) {
		AppUtil.checkConstInitialized();
		AppUtil.ensureNoDisplay();

		File confFile;
		if (SystemConf.Bool.IsDevelopmentVersion.get()) {
			confFile = new File("bin/settings-conf.txt");
		}
		else if (pathOverride != null && !pathOverride.isFile()) {
			pathOverride.mkdirs();
			confFile = new File(pathOverride, "settings-conf.txt");
		}
		else {
			File appDataDir = AppUtil.getAppDataDir();
			confFile = new File(appDataDir, "conf/settings-conf.txt");
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
						SettingsConf.Bool.FilesizeFilterMaximized.set(isVisible);
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
		filesizeForm.setText(Msg.min_max_filesize.get());
		filesizeForm.setContentsVisible(SettingsConf.Bool.FilesizeFilterMaximized.get());

		final TwoFormExpander expander = new TwoFormExpander(comp) {
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
			protected void onMaximizationChanged() {
				// Save maximization states
				MaximizedControl maxControl = getMaximizedControl();
				boolean topMax = maxControl == MaximizedControl.TOP;
				boolean bottomMax = maxControl == MaximizedControl.BOTTOM;
				SettingsConf.Bool.TypesFilterMaximized.set(topMax);
				SettingsConf.Bool.LocationFilterMaximized.set(bottomMax);
			}
		};
		expander.setTopText(Msg.document_types.get());
		if (indexRegistryLoaded) {
			expander.setBottomText(Msg.search_scope.get());
		}
		else {
			expander.setBottomText(Msg.search_scope.get() + " (" + Msg.loading.get() + ")");
			clearIndexLoadingMsg = new Runnable() {
				public void run() {
					Util.assertSwtThread();
					expander.setBottomText(Msg.search_scope.get());
				}
			};
		}
		expander.setSashWidth(sashWidth);
		
		// Restore sash weights and maximization states
		expander.setSashWeights(SettingsConf.IntArray.FilterSash.get());
		if (SettingsConf.Bool.TypesFilterMaximized.get())
			expander.setMaximizedControl(MaximizedControl.TOP);
		if (SettingsConf.Bool.LocationFilterMaximized.get())
			expander.setMaximizedControl(MaximizedControl.BOTTOM);

		// Save sash weights
		expander.getFirstControl().addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				// Run in asyncExec to make sure both controls have been resized
				Util.runAsyncExec(expander, new Runnable() {
					public void run() {
						MaximizedControl maxControl = expander.getMaximizedControl();
						if (maxControl != MaximizedControl.NONE)
							return;
						int[] weights = expander.getSashWeights();
						SettingsConf.IntArray.FilterSash.set(weights);
					}
				});
			}
		});

		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).left().top().right().applyTo(filesizeForm);
		fdf.top(filesizeForm, 5).bottom().applyTo(expander);

		return comp;
	}

	private static Control createRightTopPanel(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		searchBar = new SearchBar(comp, programConfFile);
		searchBar.evtOKClicked.add(new Event.Listener<Void> () {
		    public void update(Void eventData) {
		    	saveSettingsConfFile();
		    }
		});
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
				showManual();
			}
		});

		resultPanel.evtSelection.add(new Event.Listener<List<ResultDocument>>() {
			public void update(List<ResultDocument> eventData) {
				if (!eventData.isEmpty())
					previewPanel.setPreview(eventData.get(0));
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
				systemTrayShutdown = true;
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
				String name = SystemConf.Str.ProgramName.get();
				String version = SystemConf.Str.ProgramVersion.get();
				AppUtil.showInfo(name + " " + version);
			}
		}, new Runnable() {
			public void run() {
				PrefDialog prefDialog = new PrefDialog(shell, programConfFile);
				prefDialog.evtOKClicked.add(new Event.Listener<Void> () {
				    public void update(Void eventData) {
				    	saveSettingsConfFile();
				    }
				});
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
				previewPanel = new PreviewPanel(parent);
				previewPanel.evtHideInSystemTray.add(new Event.Listener<Void>() {
					public void update(Void eventData) {
						systemTrayHider.hide();
					}
				});
				return previewPanel;
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
	private static void initStatusBar() {
		statusBar = new StatusBar(shell) {
			public List<StatusBarPart> createRightParts(StatusBar statusBar) {
				indexingStatus = new StatusBarPart(statusBar, true);
				indexingStatus.setContents(Img.INDEXING.get(), Msg.indexing.get());
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
				webInterfaceStatus.setContents(Img.INDEXING.get(), Msg.web_interface.get());

				List<StatusBarPart> parts = new ArrayList<StatusBarPart>(2);
				parts.add(indexingStatus);
//				parts.add(webInterfaceStatus); // TODO web interface
				return parts;
			}
		};
	}

	private static void handleShellClosed(@NotNull ShellEvent e) {
		if (SettingsConf.Bool.CloseToTray.get() && !systemTrayShutdown && !Util.IS_UBUNTU_UNITY) {
			e.doit = false;
			systemTrayHider.hide();
		} else {
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
			 * Note: The getSearcher() call below will block until the searcher is
			 * available. If we run this inside the GUI thread, we won't let go of
			 * the GUI lock, causing the program to deadlock when the user tries to
			 * close the program before all indexes have been loaded.
			 */
			new Thread() {
				public void run() {
					/*
					 * The folder watcher will be null if the program is shut down
					 * while loading the indexes
					 */
					if (folderWatcher != null)
						folderWatcher.shutdown();

					if (hotkeyHandler != null)
						hotkeyHandler.shutdown();

					indexRegistry.getSearcher().shutdown();
				}
			}.start();
		}
	}

	private static void initHotkey() {
		try {
			hotkeyHandler = new HotkeyHandler();
		}
		catch (UnsupportedOperationException e) {
			return;
		}
		catch (Throwable e) {
			Util.printErr(e);
			return;
		}

		hotkeyHandler.evtHotkeyPressed.add(new Event.Listener<Void> () {
			public void update(Void eventData) {
				Util.runSyncExec(shell, new Runnable() {
					public void run() {
						if (systemTrayHider.isHidden()) {
							systemTrayHider.restore();
						}
						else {
							shell.setMinimized(false);
							shell.setVisible(true);
							shell.forceActive();
							searchBar.setFocus();
						}
					}
				});
			}
		});
		hotkeyHandler.evtHotkeyConflict.add(new Event.Listener<int[]> () {
			public void update(int[] eventData) {
				String key = UtilGui.toString(eventData);
				AppUtil.showError(Msg.hotkey_in_use.format(key), false, true);

				/*
				 * Don't open preferences dialog when the hotkey conflict occurs
				 * at startup.
				 */
				if (shell.isVisible()) {
					PrefDialog prefDialog = new PrefDialog(shell, programConfFile);
					prefDialog.evtOKClicked.add(new Event.Listener<Void> () {
					    public void update(Void eventData) {
					    	saveSettingsConfFile();
					    }
					});
					prefDialog.open();
				}
			}
		});
		hotkeyHandler.registerHotkey();
	}

	@Nullable
	private static CancelAction confirmExit() {
		MultipleChoiceDialog<CancelAction> dialog = new MultipleChoiceDialog<CancelAction>(shell);
		dialog.setTitle(Msg.abort_indexing.get());
		dialog.setText(Msg.keep_partial_index_on_exit.get());
		dialog.addButton(Msg.keep.get(), CancelAction.KEEP);
		dialog.addButton(Msg.discard.get(), CancelAction.DISCARD);
		dialog.addButton(Msg.dont_exit.get(), null);
		return dialog.open();
	}

	private static void showManual() {
		File file = ManualLocator.getManualFile();
		if (file != null) {
			if (previewPanel.setHtmlFile(file))
				threePanelForm.setSecondSubControlVisible(true);
			else
				Util.launch(file);
		}
		else {
			String msg = Msg.file_not_found.get() + "\n" + ManualLocator.manualFilename;
			AppUtil.showError(msg, true, true);
		}
	}

}
