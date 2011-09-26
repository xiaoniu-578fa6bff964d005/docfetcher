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

package net.sourceforge.docfetcher.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.model.FileResource;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.ContextMenuManager;
import net.sourceforge.docfetcher.util.gui.FileIconCache;
import net.sourceforge.docfetcher.util.gui.MenuAction;
import net.sourceforge.docfetcher.util.gui.viewer.VirtualTableViewer;
import net.sourceforge.docfetcher.util.gui.viewer.VirtualTableViewer.Column;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * @author Tran Nam Quang
 */
public final class ResultPanel {
	
	// TODO now: SWT bug: setting an image, then setting the image to null leaves an indent
	// TODO now: show an additional icon if an email has attachments
	// TODO post-release-1.1: show some helpful overlay message if a search yielded no results
	
	public enum HeaderMode {
		FILES { protected void setLabel(VariableHeaderColumn<?> column) {
			column.setLabel(column.fileHeader);
		} },
		EMAILS { protected void setLabel(VariableHeaderColumn<?> column) {
			column.setLabel(column.emailHeader);
		} },
		FILES_AND_EMAILS { protected void setLabel(VariableHeaderColumn<?> column) {
			column.setLabel(column.combinedHeader);
		} },
		;
		
		protected abstract void setLabel(@NotNull VariableHeaderColumn<?> column);
		
		@NotNull
		public static HeaderMode getInstance(boolean filesFound, boolean emailsFound) {
			final HeaderMode mode;
			if (filesFound)
				mode = emailsFound ? HeaderMode.FILES_AND_EMAILS : HeaderMode.FILES;
			else
				mode = HeaderMode.EMAILS;
			return mode;
		}
	}
	
	private static final DateFormat dateFormat = new SimpleDateFormat();
	
	public final Event<List<ResultDocument>> evtSelection = new Event<List<ResultDocument>> ();
	public final Event<Void> evtHideInSystemTray = new Event<Void>();
	
	private final VirtualTableViewer<ResultDocument> viewer;
	private final FileIconCache iconCache;
	private HeaderMode presetHeaderMode = HeaderMode.FILES; // externally suggested header mode
	private HeaderMode actualHeaderMode = HeaderMode.FILES; // header mode after examining each visible element

	public ResultPanel(@NotNull Composite parent) {
		iconCache = new FileIconCache(parent);
		
		int treeStyle = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER;
		viewer = new VirtualTableViewer<ResultDocument> (parent, treeStyle) {
			@SuppressWarnings("unchecked")
			protected List<ResultDocument> getElements(Object rootElement) {
				return (List<ResultDocument>) rootElement;
			}
		};
		
		// Open result document on double-click
		viewer.getControl().addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				List<ResultDocument> selection = viewer.getSelection();
				if (selection.isEmpty())
					return;
				ResultDocument doc = selection.get(0);
				if (!doc.isEmail())
					launchFiles(Collections.singletonList(doc));
			}
		});
		
		initContextMenu();
		
		viewer.getControl().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtSelection.fire(viewer.getSelection());
			}
		});
		
		viewer.addColumn(new VariableHeaderColumn<ResultDocument>("Title", "Subject") { // TODO i18n
			protected String getLabel(ResultDocument element) {
				return element.getTitle();
			}
			protected Image getImage(ResultDocument element) {
				if (element.isEmail())
					return Img.EMAIL.get();
				return iconCache.getIcon(element.getFilename(), Img.FILE.get());
			}
		});
		
		viewer.addColumn(new Column<ResultDocument>("Score [%]", SWT.RIGHT) { // TODO i18n
			protected String getLabel(ResultDocument element) {
				return String.valueOf(element.getScore());
			}
		});
		
		viewer.addColumn(new Column<ResultDocument>("Size", SWT.RIGHT) { // TODO i18n
			protected String getLabel(ResultDocument element) {
				return String.format("%,d KB", element.getSizeInKB());
			}
		});

		viewer.addColumn(new VariableHeaderColumn<ResultDocument>("Filename", "Sender") { // TODO i18n
			protected String getLabel(ResultDocument element) {
				if (element.isEmail())
					return element.getSender();
				return element.getFilename();
			}
			protected Image getImage(ResultDocument element) {
				return getEmailIconOrNull(element);
			}
		});

		viewer.addColumn(new Column<ResultDocument>("Type") { // TODO i18n
			protected String getLabel(ResultDocument element) {
				return element.getType();
			}
		});
		
		viewer.addColumn(new Column<ResultDocument>("Path") { // TODO i18n
			protected String getLabel(ResultDocument element) {
				return element.getPath();
			}
		});
		
		viewer.addColumn(new VariableHeaderColumn<ResultDocument>("Authors", "Sender") { // TODO i18n
			protected String getLabel(ResultDocument element) {
				return element.getAuthors();
			}
			protected Image getImage(ResultDocument element) {
				return getEmailIconOrNull(element);
			}
		});
		
		viewer.addColumn(new VariableHeaderColumn<ResultDocument>("Last Modified", "Send Date") { // TODO i18n
			protected String getLabel(ResultDocument element) {
				Date date;
				if (element.isEmail())
					date = element.getDate();
				else
					date = element.getLastModified();
				return dateFormat.format(date);
			}
			protected Image getImage(ResultDocument element) {
				return getEmailIconOrNull(element);
			}
		});
		
		SettingsConf.ColumnWidths.ResultPanel.bind(viewer.getControl());

		/*
		 * TODO now: Adjust result column headers:
		 * - Title/Subject
		 * - Filename/Sender
		 * - Last-Modified/Sent Date
		 * 
		 * make column headers movable and clickable
		 */
	}

	private void initContextMenu() {
		// TODO i18n
		
		ContextMenuManager menuManager = new ContextMenuManager(viewer.getControl());
		
		menuManager.add(new MenuAction("open") {
			public boolean isEnabled() {
				List<ResultDocument> sel = viewer.getSelection();
				if (sel.isEmpty())
					return false;
				for (ResultDocument doc : sel)
					if (doc.isEmail())
						return false;
				return true;
			}
			public void run() {
				launchFiles(viewer.getSelection());
			}
			public boolean isDefaultItem() {
				return true;
			}
		});
		
		menuManager.add(new MenuAction("open_parent") {
			public boolean isEnabled() {
				return !viewer.getSelection().isEmpty();
			}
			public void run() {
				MultiFileLauncher launcher = new MultiFileLauncher();
				for (ResultDocument doc : viewer.getSelection()) {
					String path = doc.getPath();
					try {
						launcher.addFile(getParent(path));
					}
					catch (FileNotFoundException e) {
						launcher.addMissing(path);
					}
				}
				if (launcher.launch() && SettingsConf.Bool.HideOnOpen.get())
					evtHideInSystemTray.fire(null);
				
			}
			@NotNull
			private File getParent(@NotNull String path)
					throws FileNotFoundException {
				/*
				 * The possible cases:
				 * - Path points to an ordinary file
				 * - Path points to an archive entry
				 * - Path points to an item in a PST file
				 * 
				 * In each case, the target may or may not exist.
				 */
				String[] pathParts = UtilGlobal.splitAtExisting(path);
				
				if (pathParts[1].length() == 0) // Existing ordinary file
					return Util.getParentFile(path);
				
				File leftFile = new File(pathParts[0]);
				if (leftFile.isDirectory())
					// File, archive entry or PST item does not exist
					throw new FileNotFoundException();
				
				// Existing PST item
				if (Util.hasExtension(pathParts[0], "pst"))
					return Util.getParentFile(leftFile);
				
				// Existing archive entry -> return the archive
				return leftFile;
			}
		});
	}
	
	@NotNull
	public Table getControl() {
		return viewer.getControl();
	}
	
	// header mode: auto-detect for "files + emails", no auto-detect for files and emails mode
	public void setResults(	@NotNull List<ResultDocument> results,
							@NotNull HeaderMode headerMode) {
		Util.checkNotNull(results, headerMode);
		
		if (this.presetHeaderMode != headerMode) {
			if (headerMode != HeaderMode.FILES_AND_EMAILS)
				updateColumnHeaders(headerMode);
			this.presetHeaderMode = headerMode;
		}
		setActualHeaderMode(results); // TODO now: needs some refactoring
		
		viewer.setRoot(results);
		viewer.scrollToTop();
	}
	
	private void setActualHeaderMode(List<ResultDocument> elements) {
		if (presetHeaderMode != HeaderMode.FILES_AND_EMAILS) {
			actualHeaderMode = presetHeaderMode;
			return;
		}
		boolean filesFound = false;
		boolean emailsFound = false;
		for (ResultDocument element : elements) {
			if (element.isEmail())
				emailsFound = true;
			else
				filesFound = true;
		}
		actualHeaderMode = HeaderMode.getInstance(filesFound, emailsFound);
		updateColumnHeaders(actualHeaderMode);
	}

	private void updateColumnHeaders(HeaderMode headerMode) {
		for (Column<ResultDocument> column : viewer.getColumns()) {
			if (! (column instanceof VariableHeaderColumn)) continue;
			headerMode.setLabel((VariableHeaderColumn<?>) column);
		}
	}
	
	@Nullable
	private Image getEmailIconOrNull(@NotNull ResultDocument element) {
		if (actualHeaderMode != HeaderMode.FILES_AND_EMAILS) return null;
		return element.isEmail() ? Img.EMAIL.get() : null;
	}
	
	// Should not be called with emails
	private void launchFiles(@NotNull List<ResultDocument> docs) {
		assert !docs.isEmpty();
		MultiFileLauncher launcher = new MultiFileLauncher();
		Set<FileResource> resources = new HashSet<FileResource>();
		try {
			for (ResultDocument doc : docs) {
				try {
					FileResource fileResource = doc.getFileResource();
					resources.add(fileResource);
					launcher.addFile(fileResource.getFile());
				}
				catch (FileNotFoundException e) {
					launcher.addMissing(doc.getPath());
				}
				catch (ParseException e) {
					AppUtil.showError(e.getMessage(), true, false);
					return;
				}
			}
			if (launcher.launch() && SettingsConf.Bool.HideOnOpen.get())
				evtHideInSystemTray.fire(null);
		}
		finally {
			for (FileResource fileResource : resources)
				fileResource.dispose();
		}
	}

	private static abstract class VariableHeaderColumn<T> extends Column<T> {
		private final String fileHeader;
		private final String emailHeader;
		private final String combinedHeader;
		
		public VariableHeaderColumn(@NotNull String fileHeader,
									@NotNull String emailHeader) {
			super(fileHeader);
			Util.checkNotNull(fileHeader, emailHeader);
			this.fileHeader = fileHeader;
			this.emailHeader = emailHeader;
			combinedHeader = fileHeader + " / " + emailHeader;
		}
	}

}
