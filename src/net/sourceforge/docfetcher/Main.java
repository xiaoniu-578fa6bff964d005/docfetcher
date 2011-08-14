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

package net.sourceforge.docfetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.base.ConfLoader;
import net.sourceforge.docfetcher.base.ConfLoader.Loadable;
import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.ListMap;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.gui.FormDataFactory;
import net.sourceforge.docfetcher.base.gui.LazyImageCache;
import net.sourceforge.docfetcher.base.gui.StatusManager;
import net.sourceforge.docfetcher.base.gui.StatusManager.StatusWidgetProvider;
import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.gui.FileTypePanel;
import net.sourceforge.docfetcher.gui.FilesizePanel;
import net.sourceforge.docfetcher.gui.IndexPanel;
import net.sourceforge.docfetcher.gui.ResultPanel;
import net.sourceforge.docfetcher.gui.SearchBar;
import net.sourceforge.docfetcher.gui.SearchQueue;
import net.sourceforge.docfetcher.gui.StatusBar;
import net.sourceforge.docfetcher.gui.ThreePanelForm;
import net.sourceforge.docfetcher.gui.ToolBarForm;
import net.sourceforge.docfetcher.gui.TwoFormExpander;
import net.sourceforge.docfetcher.gui.preview.PreviewPanel;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.parse.ParseService;
import net.sourceforge.docfetcher.model.parse.Parser;
import net.sourceforge.docfetcher.model.search.ResultDocument;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
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

public final class Main {

	/** The widths of the sashes in pixels */
	private static final int sashWidth = 5;

	private static volatile Display display;
	private static Shell shell;
	private static volatile IndexRegistry indexRegistry;
	
	private static FilesizePanel filesizePanel;
	private static FileTypePanel fileTypePanel;
	private static IndexPanel indexPanel;

	private static SearchBar searchBar;
	private static ResultPanel resultPanel;
	private static PreviewPanel previewPanel;

	private Main() {
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

		// TODO Initialize MsgUtil and AppUtil.Msg, should be done as early as
		// possible
		// TODO See what happens if we don't initialize all constants
		AppUtil.Const.PROGRAM_NAME.set(SystemConf.Str.ProgramName.get());
		AppUtil.Const.PROGRAM_VERSION.set(SystemConf.Str.ProgramVersion.get());
		AppUtil.Const.PROGRAM_BUILD_DATE.set(SystemConf.Str.BuildDate.get());
		AppUtil.Const.USER_DIR_PATH.set(Util.USER_DIR_PATH);
		AppUtil.Const.IS_PORTABLE.set(SystemConf.Bool.IsPortable.get());
		AppUtil.Const.IS_DEVELOPMENT_VERSION.set(SystemConf.Bool.IsDevelopmentVersion.get());

		if (!AppUtil.checkSingleInstance()) return;

		// TODO load settings after starting the index registry thread ->
		// values for index registry constructor can be set later
		
		// Load program configuration and preferences; load index registry
		loadProgramConf();
		File settingsConfFile = loadSettingsConf();
		loadIndexRegistry();

		display = new Display();
		shell = new Shell(display);
		AppUtil.setDisplay(display);

		// Load images
		LazyImageCache lazyImageCache = new LazyImageCache(
			display, SystemConf.Str.ImgDir.get());
		Img.initialize(lazyImageCache);
		lazyImageCache.reportMissingFiles(
			shell, Img.class, "Missing image files:");

		// Set shell icons, must be done *after* loading the images
		shell.setImages(new Image[] {
			Img.DOCFETCHER_16.get(), Img.DOCFETCHER_32.get(),
			Img.DOCFETCHER_48.get(), });

		// Set default uncaught exception handler
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, final Throwable e) {
				AppUtil.showStackTrace(e);
			}
		});

		SettingsConf.ShellBounds.MainWindow.bind(shell);
		SettingsConf.Bool.MainShellMaximized.bindMaximized(shell);
		shell.setLayout(new FormLayout());
		shell.setText(ProgramConf.Str.AppName.get());

		ThreePanelForm threePanelForm = new ThreePanelForm(shell, 250) {
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
		
		new SearchQueue(
			searchBar, filesizePanel, fileTypePanel, indexPanel, resultPanel);

		final StatusBar statusBar = new StatusBar(shell) {
			public List<StatusBarPart> createRightParts(StatusBar statusBar) {
				StatusBarPart part1 = new StatusBarPart(statusBar);
				part1.setContents(Img.INDEXING.get(), "Indexing...");
				StatusBarPart part2 = new StatusBarPart(statusBar);
				part2.setContents(Img.INDEXING.get(), "Web Interface");

				List<StatusBarPart> parts = new ArrayList<StatusBarPart>(2);
				parts.add(part1);
				parts.add(part2);
				return parts;
			}
		};
		statusBar.getLeftPart().setContents(Img.INDEXING.get(), "Status Bar");

		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.bottom().left().right().applyTo(statusBar);
		fdf.top().bottom(statusBar).applyTo(threePanelForm);

		new StatusManager(display, new StatusWidgetProvider() {
			public String getStatus() {
				return statusBar.getLeftPart().getText();
			}

			public void setStatus(String text) {
				statusBar.getLeftPart().setContents(null, text);
			}
		});
		
		// Move focus to search text field
		searchBar.setFocus();

		// Global keyboard shortcuts
		display.addFilter(SWT.KeyUp, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				// TODO global keys
			}
		});
		
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(final ShellEvent e) {
				e.doit = indexRegistry.getQueue().shutdown(new CancelHandler() {
					public CancelAction cancel() {
						// TODO Ask for confirmation:
						// Discard, Keep, Don't Exit
						// -> Refactor KeepDiscardDialog into generic 3-button confirmation dialog
						return null;
					}
				});
				
				if (e.doit)
					indexRegistry.getSearcher().shutdown();
			}
		});

		// TODO mark classes in 'view' package as final / package-visible when
		// possible
		// -> move GUI classes above into view package

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

	private static void loadIndexRegistry() {
		File indexParentDir;
		if (SystemConf.Bool.IsDevelopmentVersion.get())
			indexParentDir = new File("bin", "indexes");
		else if (SystemConf.Bool.IsPortable.get())
			indexParentDir = new File("indexes");
		else
			indexParentDir = AppUtil.getAppDataDir();
		
		// TODO make cache capacity customizable
		int reporterCapacity = ProgramConf.Int.MaxLinesInProgressPanel.get();
		indexRegistry = new IndexRegistry(indexParentDir, 20, reporterCapacity);
		
		new Thread(Main.class.getName() + " (load index registry)") {
			public void run() {
				try {
					indexRegistry.load(new Cancelable() {
						public boolean isCanceled() {
							return display != null && display.isDisposed();
						}
					});
				}
				catch (IOException e) {
					// Wait until the display is available
					int tries = 0;
					while (display == null && tries < 100) {
						tries++;
						try {
							Thread.sleep(100);
						}
						catch (InterruptedException e1) {
							break;
						}
					}
					AppUtil.showStackTrace(e);
				}
			}
		}.start();
	}

	private static void loadProgramConf() {
		String programConfFilename = SystemConf.Str.ProgramConfFilename.get();
		File appDataDir = AppUtil.getAppDataDir();
		File programConfFile = new File(appDataDir, programConfFilename);
		try {
			List<Loadable> notLoaded = ConfLoader.load(
				programConfFile, ProgramConf.class, false);
			if (!notLoaded.isEmpty()) {
				List<String> entryNames = new ArrayList<String>(
					notLoaded.size());
				for (Loadable entry : notLoaded)
					entryNames.add("  " + entry.name());
				String msg = String.format(
					"The following entries in '%s' "
							+ "are missing or have invalid values:\n",
					programConfFilename);
				msg += Joiner.on("\n").join(entryNames);
				AppUtil.showErrorOnStart(msg, false);
			}
		}
		catch (FileNotFoundException e) {
			// Restore conf file if missing
			String absPath = Util.getSystemAbsPath(programConfFile);
			String msg = String.format("Configuration file is missing:\n%s.\n"
					+ "File will be restored.", absPath);
			AppUtil.showErrorOnStart(msg, false);
			InputStream in = Main.class.getResourceAsStream(programConfFilename);
			try {
				ConfLoader.load(in, ProgramConf.class);
				URL url = Resources.getResource(Main.class, programConfFilename);
				Files.copy(
					Resources.newInputStreamSupplier(url), programConfFile);
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
		File appDataDir = AppUtil.getAppDataDir();
		String settingsConfName = SystemConf.Str.SettingsConfFilename.get();
		File settingsConfFile = new File(appDataDir, settingsConfName);
		try {
			ConfLoader.load(settingsConfFile, SettingsConf.class, true);
		}
		catch (IOException e) {
			AppUtil.showStackTraceInOwnDisplay(e);
		}
		return settingsConfFile;
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
				// TODO move this method to GuiUtil?
				// UtilDF.addMouseHighlighter(item);
				return item;
			}

			protected Control createContents(Composite parent) {
				filesizePanel = new FilesizePanel(parent);
				return filesizePanel.getControl();
			}
		};
		filesizeForm.setImage(Img.INDEXING.get());
		filesizeForm.setText("Minimum / Maximum Filesize");

		TwoFormExpander expander = new TwoFormExpander(comp) {
			protected Control createFirstContents(Composite parent) {
				// TODO Load parser states from file
				// TODO Save parser states to file?
				List<Parser> parsers = ParseService.getParsers();
				ListMap<Parser, Boolean> map = ListMap.create(parsers.size());
				for (Parser parser : parsers)
					map.add(parser, true);
				fileTypePanel = new FileTypePanel(parent, map);
				return fileTypePanel.getControl();
			}

			protected Control createSecondContents(Composite parent) {
				indexPanel = new IndexPanel(parent, indexRegistry);
				return indexPanel.getControl();
			}
		};
		expander.setTopImage(Img.INDEXING.get());
		expander.setBottomImage(Img.INDEXING.get());
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

		resultPanel.evtSelection.add(new Event.Listener<List<ResultDocument>>() {
			public void update(List<ResultDocument> eventData) {
				if (eventData.isEmpty()) return;
				previewPanel.setPreview(eventData.get(0));
				// TODO update status bar
			}
		});

		return comp;
	}

}
