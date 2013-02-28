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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.model.FileResource;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.Path.PathParts;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.collect.AlphanumComparator;
import net.sourceforge.docfetcher.util.gui.ContextMenuManager;
import net.sourceforge.docfetcher.util.gui.FileIconCache;
import net.sourceforge.docfetcher.util.gui.MenuAction;
import net.sourceforge.docfetcher.util.gui.viewer.VirtualTableViewer;
import net.sourceforge.docfetcher.util.gui.viewer.VirtualTableViewer.Column;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.google.common.primitives.Longs;

/**
 * @author Tran Nam Quang
 */
public final class ResultPanel {
	
	// TODO post-release-1.1: show an additional icon if an email has attachments
	// TODO post-release-1.1: show some helpful overlay message if a search yielded no results
	// TODO post-release-1.1: implement context menu: copy paths to clipboard, including Mod1+C shortcut (also: Mod1+A for selecting all items)
	
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
		Table table = viewer.getControl();
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				launchSelection();
			}
		});
		
		table.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (Util.isEnterKey(e.keyCode))
					launchSelection();
			}
		});
		
		viewer.setSortingEnabled(true);
		initContextMenu();
		
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtSelection.fire(viewer.getSelection());
			}
		});
		
		viewer.addColumn(new VariableHeaderColumn<ResultDocument>(Msg.title.get(), Msg.subject.get()) {
			protected String getLabel(ResultDocument element) {
				return element.getTitle();
			}
			protected Image getImage(ResultDocument element) {
				if (element.isEmail())
					return Img.EMAIL.get();
				return iconCache.getIcon(element.getFilename(), Img.FILE.get());
			}
			protected int compare(ResultDocument e1, ResultDocument e2) {
				return compareAlphanum(e1.getTitle(), e2.getTitle());
			}
		});
		
		viewer.addColumn(new Column<ResultDocument>(Msg.score.get(), SWT.RIGHT) {
			protected String getLabel(ResultDocument element) {
				return String.valueOf(element.getScore());
			}
			protected int compare(ResultDocument e1, ResultDocument e2) {
				return -1 * Float.compare(e1.getScore(), e2.getScore());
			}
		});
		
		viewer.addColumn(new Column<ResultDocument>(Msg.size.get(), SWT.RIGHT) {
			protected String getLabel(ResultDocument element) {
				return String.format("%,d KB", element.getSizeInKB());
			}
			protected int compare(ResultDocument e1, ResultDocument e2) {
				return -1 * Longs.compare(e1.getSizeInKB(), e2.getSizeInKB());
			}
		});

		viewer.addColumn(new VariableHeaderColumn<ResultDocument>(Msg.filename.get(), Msg.sender.get()) {
			protected String getLabel(ResultDocument element) {
				if (element.isEmail())
					return element.getSender();
				return element.getFilename();
			}
			protected int compare(ResultDocument e1, ResultDocument e2) {
				return compareAlphanum(getLabel(e1), getLabel(e2));
			}
		});

		viewer.addColumn(new Column<ResultDocument>(Msg.type.get()) {
			protected String getLabel(ResultDocument element) {
				return element.getType();
			}
			protected int compare(ResultDocument e1, ResultDocument e2) {
				return compareAlphanum(e1.getType(), e2.getType());
			}
		});
		
		viewer.addColumn(new Column<ResultDocument>(Msg.path.get()) {
			protected String getLabel(ResultDocument element) {
				return element.getPath().getPath();
			}
			protected int compare(ResultDocument e1, ResultDocument e2) {
				return compareAlphanum(getLabel(e1), getLabel(e2));
			}
		});
		
		viewer.addColumn(new VariableHeaderColumn<ResultDocument>(Msg.authors.get(), Msg.sender.get()) {
			protected String getLabel(ResultDocument element) {
				return element.getAuthors();
			}
			protected int compare(ResultDocument e1, ResultDocument e2) {
				return compareAlphanum(e1.getAuthors(), e2.getAuthors());
			}
		});
		
		viewer.addColumn(new VariableHeaderColumn<ResultDocument>(Msg.last_modified.get(), Msg.send_date.get()) {
			protected String getLabel(ResultDocument element) {
				Date date = getDate(element);
				return date == null ? "" : dateFormat.format(date);
			}
			protected int compare(ResultDocument e1, ResultDocument e2) {
				Date date1 = getDate(e1);
				Date date2 = getDate(e2);
				if (date1 == null) // Place null dates before non-null dates
					return date2 == null ? 0 : -1;
				else if (date2 == null)
					return 1;
				return date1.compareTo(date2);
			}
			@Nullable
			private Date getDate(ResultDocument element) {
				if (element.isEmail())
					return element.getDate();
				return element.getLastModified();
			}
		});
		
		SettingsConf.ColumnWidths.ResultPanel.bind(table);
		SettingsConf.ColumnOrder.ResultPanelColumnOrder.bind(table);
	}
	
	private void launchSelection() {
		List<ResultDocument> selection = viewer.getSelection();
		if (selection.isEmpty())
			return;
		ResultDocument doc = selection.get(0);
		if (!doc.isEmail())
			launchFiles(Collections.singletonList(doc));
	}
	
	private static int compareAlphanum(@NotNull String s1, @NotNull String s2) {
		return AlphanumComparator.ignoreCaseInstance.compare(s1, s2);
	}

	private void initContextMenu() {
		ContextMenuManager menuManager = new ContextMenuManager(viewer.getControl());
		
		menuManager.add(new MenuAction(Msg.open.get()) {
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
		
		menuManager.add(new MenuAction(Msg.open_parent.get()) {
			public boolean isEnabled() {
				return !viewer.getSelection().isEmpty();
			}
			public void run() {
				MultiFileLauncher launcher = new MultiFileLauncher();
				for (ResultDocument doc : viewer.getSelection()) {
					Path path = doc.getPath();
					try {
						launcher.addFile(getParent(path));
					}
					catch (FileNotFoundException e) {
						launcher.addMissing(path.getCanonicalPath());
					}
				}
				if (launcher.launch() && SettingsConf.Bool.HideOnOpen.get())
					evtHideInSystemTray.fire(null);
			}
			@NotNull
			private File getParent(@NotNull Path path)
					throws FileNotFoundException {
				/*
				 * The possible cases:
				 * - Path points to an ordinary file
				 * - Path points to an archive entry
				 * - Path points to an item in a PST file
				 * 
				 * In each case, the target may or may not exist.
				 */
				PathParts pathParts = path.splitAtExistingFile();
				
				if (pathParts.getRight().isEmpty()) // Existing ordinary file
					return Util.getParentFile(path.getCanonicalFile());
				
				File leftFile = pathParts.getLeft().getCanonicalFile();
				if (leftFile.isDirectory())
					// File, archive entry or PST item does not exist
					throw new FileNotFoundException();
				
				// Existing PST item
				if (Util.hasExtension(pathParts.getLeft().getName(), "pst"))
					return Util.getParentFile(leftFile);
				
				// Existing archive entry -> return the archive
				return leftFile;
			}
		});
		
		menuManager.addSeparator();
		
		menuManager.add(new MenuAction(Msg.copy.get()) {
			public boolean isEnabled() {
				return !viewer.getSelection().isEmpty();
			}
			public void run() {
				List<ResultDocument> docs = getSelection();
				List<File> files = new ArrayList<File>(docs.size());
				for (ResultDocument doc : docs)
					files.add(doc.getPath().getCanonicalFile());
				Util.setClipboard(files);
			}
		});
	}
	
	@NotNull
	public Table getControl() {
		return viewer.getControl();
	}
	
	public int getItemCount() {
		return viewer.getControl().getItemCount();
	}
	
	@MutableCopy
	@NotNull
	public List<ResultDocument> getSelection() {
		return viewer.getSelection();
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
		setActualHeaderMode(results); // TODO post-release-1.1: needs some refactoring
		
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

	// sign of given index specifies direction of sorting
	// zero and out-of-range values will be ignored
	// column numbering starts at 1
	// the index points at the column in visual order, not in creation order
	public void sortByColumn(int columnIndex) {
		if (columnIndex == 0)
			return;
		/*
		 * Note: The column to sort by must be specified as an index, since the
		 * column names may change.
		 */
		try {
			int index = Math.abs(columnIndex) - 1;
			List<Column<ResultDocument>> columns = viewer.getColumnsVisualOrder();
			if (index >= columns.size())
				return;
			boolean up = Math.signum(columnIndex) > 0;
			viewer.sortByColumn(columns.get(index), up);
		}
		catch (NumberFormatException e) {
			return;
		}
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
					launcher.addMissing(doc.getPath().getCanonicalPath());
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
