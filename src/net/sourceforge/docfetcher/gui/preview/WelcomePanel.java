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

import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
public final class WelcomePanel extends Composite {

	public static void main(String[] args) {
		AppUtil.Const.autoInit();
		
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(Util.createFillLayout(0));
		shell.setText("Welcome");
		Util.setCenteredBounds(shell, 600, 600);

		new WelcomePanel(shell).evtLinkClicked.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				AppUtil.showInfo("Link to manual was clicked.");
			}
		});

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
	public final Event<Void> evtLinkClicked = new Event<Void> ();
	
	private final Image image;
	private final Font font;
	
	public WelcomePanel(@NotNull Composite parent) {
		super(parent, SWT.NONE);
		setLayout(Util.createGridLayout(1, false, 0, 0));
		
		// A container to center the entire drawing
		Composite centerComp = new Composite(this, SWT.NONE);
		centerComp.setBackground(Col.WHITE.get());
		centerComp.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		centerComp.setLayout(Util.createGridLayout(2, false, 0, 0));
		
		// Load resources
		String imgDir = SystemConf.Str.ImgDir.get();
		image = new Image(getDisplay(), imgDir + "/squirrel.png");
		font = new Font(getDisplay(), "Verdana", 10, SWT.NORMAL);
		
		// Dispose resources
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				image.dispose();
				font.dispose();
			}
		});
		
		/*
		 * The label that contains the squirrel image; no layout data is set on
		 * the label, which (mysteriously) causes it to be always on top.
		 */
		Label imageLabel = new Label(centerComp, SWT.NONE);
		imageLabel.setImage(image);
		
		// A container for the text widget
		final Composite textComp = new Composite(centerComp, SWT.NONE);
		textComp.setBackground(Col.WHITE.get());
		textComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		textComp.setLayout(Util.createGridLayout(1, false, 20, 0));
		
		// TODO i18n
		
		String msg = "Greetings! If you are new to" +
				" DocFetcher please follow the" +
				" instructions in the <a>manual</a>.";
		
		msg = "Willkommen! Wenn Sie DocFetcher"
				+ " zum ersten Mal benutzen,"
				+ " folgen Sie bitte den Anweisungen"
				+ " im <a>Handbuch</a>.";
		
		// TODO parse HTML link, store link offsets
		// fire link click event
		// change mouse cursor when hovering over link
		
		// The text widget
		StyledText text = new StyledText(textComp, SWT.WRAP);
		text.setEnabled(false);
		text.setText(msg);
		text.setFont(font);
		text.setBackground(Col.WHITE.get());
		GridData linkData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		linkData.horizontalIndent = 50;
		linkData.widthHint = 200;
		text.setLayoutData(linkData);
		
		// Fire event when link is clicked
		text.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtLinkClicked.fire(null);
			}
		});
		
		// Draw a balloon border around the text widget
		textComp.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.setLineWidth(2);
				e.gc.setAntialias(SWT.ON);
				
				// Draw a rounded border
				e.gc.setForeground(Col.GRAY.get());
				e.gc.drawRoundRectangle(
						// margin = 20 = 5 + 15 = [outer margin] + [inner margin]
						55, // x = 20 + 50 - 15 = [margin] + [indent] - [inner margin]
						5, // y = [outer margin]
						e.width - 60, // width = e.width - 2 * 20 - 50 + 2 * 15
										// = [composite width] - 2 * [margin] - [indent] + 2 * [inner margin]
						e.height - 10, // height = e.height - 2 * 5 = [composite height] - 2 * [outer margin]
						50, // arc width
						50 // arc height
				);

				/*
				 * Draw an invisible, filled rectangle to cover the portion of
				 * the left side of the rounded border where we're going to
				 * attach a triangle.
				 */
				e.gc.setBackground(Col.WHITE.get());
				e.gc.fillRectangle(
						50, // x = 55 - 5 = [margin] + [indent] - [inner margin] - 5
						35, // y = 20 + 15 = [margin] + 15
						10, // width = 5 + 5
						25 // height = 25
				);
				
				// Draw a triangle on the left side of the rounded border
				int[] points = new int[] {
						55, // x1 = 20 + 50 - 15 = [margin] + [indent] - [inner margin]
						35, // y1 = 20 + 15 = [margin] + 15
						20, // x2 = [margin]
						50, // y2 = 20 + 15 + 15 = [margin] + 15 + 15
						55, // x3 = x1
						60 // y3 = 20 + 15 + 25 = [margin] + 15 + 25
				};
				e.gc.setForeground(Col.GRAY.get());
				e.gc.drawPolyline(points);
			}
		});
		
		// This fixes some painting artifacts that occur when the widget is resized
		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				textComp.redraw();
			}
		});
	}

}
