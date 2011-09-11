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

package net.sourceforge.docfetcher.gui.preview;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.gui.Col;
import net.sourceforge.docfetcher.base.gui.ToolItemFactory;
import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.gui.CustomBorderComposite;
import net.sourceforge.docfetcher.gui.UtilGui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author Tran Nam Quang
 */
final class HtmlPreview extends ToolBarForm {
	
	@NotNull private ToolItem backBt;
	@NotNull private ToolItem forwardBt;
	@NotNull private Text locationBar;
	@NotNull private Browser browser;
	
	public HtmlPreview(@NotNull Composite parent) {
		super(parent);
	}
	
	@NotNull
	protected Control createToolBar(@NotNull Composite parent) {
		CustomBorderComposite comp = new CustomBorderComposite(parent);
		comp.setLayout(Util.createGridLayout(3, false, 0, 0));
		
		ToolBar toolBar = new ToolBar(comp, SWT.FLAT);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		// TODO i18n for all button tooltips
		
		ToolItemFactory tif = new ToolItemFactory(toolBar);
		tif.enabled(false);
		
		backBt = tif.image(Img.ARROW_LEFT.get()).toolTip("prev_page")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						browser.back();
					}
				}).create();
		
		forwardBt = tif.image(Img.ARROW_RIGHT.get()).toolTip("next_page")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						browser.forward();
					}
				}).create();
		
		tif.enabled(true);
		
		tif.image(Img.STOP.get()).toolTip("browser_stop")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						browser.stop();
					}
				}).create();
		
		tif.image(Img.REFRESH.get()).toolTip("browser_refresh")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						browser.refresh();
					}
				}).create();

		tif.image(Img.WINDOW.get()).toolTip("browser_launch_external")
				.listener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						String url = browser.getUrl();
						if (url.equals(""))
							return;
						Util.launch(url);
						// TODO minimize DocFetcher to system tray
						// (depends on program settings)
					}
				}).create();
		
		locationBar = new Text(comp, SWT.SINGLE | SWT.BORDER);
		locationBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Util.selectAllOnFocus(locationBar);
		
		locationBar.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (UtilGui.isEnterKey(e.keyCode))
					browser.setUrl(locationBar.getText());
			}
		});
		
		return comp;
	}
	
	@NotNull
	protected Control createContents(@NotNull Composite parent) {
		// TODO Add program.conf entry to allow using Mozilla (SWT.MOZILLA) or WebKit browser
		browser = new Browser(parent, SWT.BORDER);
		
		browser.addLocationListener(new LocationAdapter() {
			public void changing(LocationEvent event) {
				locationBar.setBackground(Col.WIDGET_BACKGROUND.get());
			}
			public void changed(LocationEvent event) {
				backBt.setEnabled(browser.isBackEnabled());
				forwardBt.setEnabled(browser.isForwardEnabled());
				String path = browser.getUrl();
				
				if (path.equals("about:blank")) {
					path = "";
				}
				else if (path.startsWith("file:///")) { //$NON-NLS-1$
					try {
						path = Util.getSystemAbsPath(new File(new URI(path)));
					}
					catch (Exception e) {
						/*
						 * Ignoring URISyntaxException and
						 * IllegalArgumentException. The latter can happen if
						 * the URI contains a "fragment component", e.g.
						 * "myfile.htm#Section_1".
						 */
					}
				}
				locationBar.setText(path);
	            
				/*
				 * The appropriate color is 'LIST_BACKGROUND', not 'WHITE',
				 * because the user might have chosen a dark theme.
				 */
	            locationBar.setBackground(Col.LIST_BACKGROUND.get());
			}
		});
		
		return browser;
	}
	
	// TODO maybe add HTML highlighting
	/**
	 * Sets the file to be displayed.
	 */
	public void setFile(@NotNull File file) {
		String path = Util.getSystemAbsPath(file);
		try {
			String url = file.toURI().toURL().toString();
			browser.setUrl(url);
		}
		catch (MalformedURLException e) {
			browser.setUrl(path);
		}
		locationBar.setText(path);
	}

	public void clear() {
		browser.setText("");
	}

}
