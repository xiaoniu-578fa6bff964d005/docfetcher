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
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SettingsConf.FontDescription;
import net.sourceforge.docfetcher.gui.UtilGui;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.util.annotations.ThreadSafe;
import net.sourceforge.docfetcher.util.collect.AlphanumComparator;
import net.sourceforge.docfetcher.util.collect.ListMap;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.ContextMenuManager;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;
import net.sourceforge.docfetcher.util.gui.MenuAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
final class FileExtensionChooser {
	
	public static final class Factory {
		private final Shell parentShell;
		private final File rootDir;
		@Nullable private Set<String> cachedExtensions; // should only be accessed from the SWT thread
		
		public Factory(@NotNull Shell parentShell, @NotNull File rootDir) {
			Util.checkNotNull(parentShell, rootDir);
			this.parentShell = parentShell;
			this.rootDir = rootDir;
		}
		
		@NotNull
		public FileExtensionChooser createChooser() {
			return new FileExtensionChooser(this);
		}
	}

	private final Factory factory;
	private final Shell shell;
	private final Table table;
	private final Composite comp;
	private final StackLayout stackLayout;
	private final Button okBt;
	
	@Nullable private Thread thread;
	@Nullable private ListMap<String, Boolean> output;
	
	private FileExtensionChooser(@NotNull Factory factory) {
		// TODO i18n
		this.factory = factory;
		shell = new Shell(factory.parentShell, UtilGui.DIALOG_STYLE);
		
		Label label = new Label(shell, SWT.NONE);
		label.setText("select_exts");
		
		comp = new Composite(shell, SWT.NONE);
		stackLayout = new StackLayout();
		comp.setLayout(stackLayout);
		table = new Table(comp, SWT.CHECK | SWT.HIDE_SELECTION | SWT.BORDER);
		Composite textContainer = new Composite(comp, SWT.BORDER);
		textContainer.setBackground(Col.LIST_BACKGROUND.get()); // don't use WHITE, it won't work with dark themes
		textContainer.setLayout(Util.createFillLayout(5));
		
		StyledText loadingMsg = new StyledText(textContainer, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
		loadingMsg.setBackground(Col.LIST_BACKGROUND.get()); // don't use WHITE, it won't work with dark themes
		loadingMsg.setText("loading");
		if (Util.IS_WINDOWS)
			loadingMsg.setFont(FontDescription.PreviewWindows.get());
		else
			loadingMsg.setFont(FontDescription.PreviewLinux.get());
		loadingMsg.getCaret().setVisible(false);
		stackLayout.topControl = textContainer;
		
		okBt = new Button(shell, SWT.PUSH);
		okBt.setText("ok");
		Button cancelBt = new Button(shell, SWT.PUSH);
		cancelBt.setText("cancel");
		
		shell.setLayout(Util.createFormLayout(5));
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top().left().right().applyTo(label);
		fdf.reset().minWidth(75).bottom().right().applyTo(cancelBt);
		fdf.right(cancelBt).applyTo(okBt);
		fdf.reset().left().right().top(label).bottom(okBt).applyTo(comp);
		
		okBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				output = ListMap.create(table.getItemCount());
				for (TableItem item : table.getItems())
					output.add(item.getText(), item.getChecked());
				shell.close();
			}
		});
		
		cancelBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				if (thread != null)
					thread.interrupt();
			}
		});
		
		initContextMenu();
	}

	private void initContextMenu() {
		class CheckAllAction extends MenuAction {
			private final boolean value;

			private CheckAllAction(boolean value) {
				super(value ? "check_all" : "uncheck_all");
				this.value = value;
			}
			public void run() {
				for (TableItem item : table.getItems())
					item.setChecked(value);
			}
		}
		
		ContextMenuManager menuManager = new ContextMenuManager(table);
		menuManager.add(new CheckAllAction(true));
		menuManager.add(new CheckAllAction(false));
		
		menuManager.addSeparator();
		
		menuManager.add(new MenuAction("invert_selection") {
			public void run() {
				for (TableItem item : table.getItems())
					item.setChecked(!item.getChecked());
			}
		});
	}
	
	// returns table check states or an empty map if the loading was cancelled
	@Nullable
	public ListMap<String, Boolean> open(@NotNull final Collection<String> checkedExtensions)
			throws FileNotFoundException {
		Util.checkNotNull(checkedExtensions);
		Util.assertSwtThread();
		
		if (!factory.rootDir.isDirectory())
			throw new FileNotFoundException();
		
		if (factory.cachedExtensions != null) {
			showExtensions(checkedExtensions);
		}
		else {
			okBt.setEnabled(false);
			thread = new Thread(FileExtensionChooser.class.getSimpleName()) {
				public void run() {
					final Set<String> cachedExtensions = listExtensions(factory.rootDir);
					if (Util.isInterrupted())
						return;
					Util.runAsyncExec(table, new Runnable() {
						public void run() {
							if (factory.cachedExtensions == null)
								factory.cachedExtensions = cachedExtensions;
							showExtensions(checkedExtensions);
							okBt.setEnabled(true);
						}
					});
				}
			};
			thread.start();
		}
		
		SettingsConf.ShellBounds.FileExtensionChooser.bind(shell);
		shell.open();
		
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return output;
	}
	
	@ThreadSafe
	private void showExtensions(@NotNull Collection<String> checkedExtensions) {
		Util.assertSwtThread();
		for (String ext : factory.cachedExtensions) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(ext);
			if (checkedExtensions.contains(ext))
				item.setChecked(true);
		}
		stackLayout.topControl = table;
		comp.layout();
	}
	
	/**
	 * Recursively collects all file extensions under the given directory and
	 * then sorts and returns them. Files with an extension, but no basename
	 * (e.g. ".classpath") are omitted.
	 */
	@NotNull
	private static Set<String> listExtensions(@NotNull File rootDir) {
		Set<String> exts = new TreeSet<String>(AlphanumComparator.ignoreCaseInstance);
		listExtensions(exts, rootDir);
		return exts;
	}
	
	/**
	 * Recursively collects all file extensions under the given directory and
	 * puts them into the given Set. Files with an extension, but no basename
	 * (e.g. ".classpath") are omitted.
	 * <p>
	 * Does nothing if the given directory cannot be accessed.
	 */
	@RecursiveMethod
	private static void listExtensions(	@NotNull Set<String> exts,
										@NotNull File rootDir) {
		for (File file : Util.listFiles(rootDir)) {
			if (Util.isInterrupted())
				return;
			if (file.isFile()) {
				String ext = Util.getExtension(file);
				if (ext.trim().equals("")) //$NON-NLS-1$
					continue;
				
				// skip files with extension, but no basename (e.g. ".classpath")
				if (ext.length() + 1 == file.getName().length())
					continue;
				
				exts.add(ext);
			}
			else if (file.isDirectory() && ! Util.isJunctionOrSymlink(file))
				listExtensions(exts, file);
		}
	}

}
