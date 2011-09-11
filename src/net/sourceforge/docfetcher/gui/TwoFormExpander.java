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

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public abstract class TwoFormExpander extends Composite {
	
	@NotNull private Label itemLeftTop0;
	@NotNull private Label itemRightTop0;
	@NotNull private Label itemLeftTop;
	@NotNull private Label itemRightTop;
	@NotNull private Label itemLeftBottom;
	@NotNull private Label itemRightBottom;
	@NotNull private Label itemLeftBottom0;
	@NotNull private Label itemRightBottom0;
	@NotNull private SashForm sash;
	@NotNull private ToolBarForm formTop;
	@NotNull private ToolBarForm formBottom;
	@NotNull private ToolBarFormHeader formTop0;
	@NotNull private ToolBarFormHeader formBottom0;
	
	// TODO store sash weights in maximized state, persist maximized state
	
	public TwoFormExpander(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(Util.createGridLayout(1, false, 0, 5));
		
		formTop0 = new ToolBarFormHeader(this) {
			protected Control createToolBar(Composite parent) {
				return TwoFormExpander.this.createToolBar(parent, true, true);
			}
		};
		formTop0.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sash = new SashForm(this, SWT.VERTICAL | SWT.SMOOTH);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		formTop = new ToolBarForm(sash) {
			@Nullable
			protected Control createToolBar(Composite parent) {
				return TwoFormExpander.this.createToolBar(parent, true, false);
			}
			@NotNull
			protected Control createContents(Composite parent) {
				return createFirstContents(parent);
			}
		};
		
		formBottom = new ToolBarForm(sash) {
			@Nullable
			protected Control createToolBar(Composite parent) {
				return TwoFormExpander.this.createToolBar(parent, false, false);
			}
			@NotNull
			protected Control createContents(Composite parent) {
				return createSecondContents(parent);
			}
		};
		
		formBottom0 = new ToolBarFormHeader(this) {
			@Nullable
			protected Control createToolBar(Composite parent) {
				return TwoFormExpander.this.createToolBar(parent, false, true);
			}
		};
		formBottom0.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		updateLayout();
		
		MouseAdapter restoreHandler = new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				sash.setMaximizedControl(null);
				updateLayout();
			}
		};
		
		MouseAdapter minimizeTopHandler = new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				sash.setMaximizedControl(formBottom);
				updateLayout();
			}
		};
		
		MouseAdapter maximizeTopHandler = new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				sash.setMaximizedControl(formTop);
				updateLayout();
			}
		};
		
		itemLeftTop0.addMouseListener(restoreHandler);
		itemRightTop0.addMouseListener(maximizeTopHandler);
		
		itemLeftTop.addMouseListener(minimizeTopHandler);
		
		itemRightTop.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				if (sash.getMaximizedControl() == null)
					sash.setMaximizedControl(formTop);
				else if (sash.getMaximizedControl() == formTop)
					sash.setMaximizedControl(null);
				updateLayout();
			}
		});
		
		itemLeftBottom.addMouseListener(maximizeTopHandler);
		
		itemRightBottom.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				if (sash.getMaximizedControl() == null)
					sash.setMaximizedControl(formBottom);
				else if (sash.getMaximizedControl() == formBottom)
					sash.setMaximizedControl(null);
				updateLayout();
			}
		});
		
		itemLeftBottom0.addMouseListener(restoreHandler);
		itemRightBottom0.addMouseListener(minimizeTopHandler);
		
		Label[] items = {
				itemLeftTop0, itemRightTop0,
				itemLeftTop, itemRightTop,
				itemLeftBottom, itemRightBottom,
				itemLeftBottom0, itemRightBottom0,
		};
		for (final Label item : items)
			UtilGui.addMouseHighlighter(item);
	}
	
	@NotNull
	private Control createToolBar(Composite parent, boolean isTop, boolean isHeader) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(Util.createGridLayout(2, false, 0, 2));
		Label item1 = new Label(comp, SWT.PUSH);
		Label item2 = new Label(comp, SWT.PUSH);
		item1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		item2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (isTop) {
			if (isHeader) {
				itemLeftTop0 = item1;
				itemRightTop0 = item2;
			} else {
				itemLeftTop = item1;
				itemRightTop = item2;
			}
		} else {
			if (isHeader) {
				itemLeftBottom0 = item1;
				itemRightBottom0 = item2;
			} else {
				itemLeftBottom = item1;
				itemRightBottom = item2;
			}
		}
		return comp;
	}
	
	/**
	 * Returns the width of the sash.
	 */
	public final int getSashWidth() {
		return sash.getSashWidth();
	}
	
	/**
	 * Sets the width of the sash.
	 */
	public final void setSashWidth(int width) {
		sash.setSashWidth(width);
	}
	
	private void updateLayout() {
		Control max = sash.getMaximizedControl();
		if (max == null) {
			itemLeftTop.setImage(Img.MINIMIZE.get());
			itemRightTop.setImage(Img.MAXIMIZE.get());
			itemLeftBottom.setImage(Img.MINIMIZE.get());
			itemRightBottom.setImage(Img.MAXIMIZE.get());
			setVisible(formTop0, false);
			setVisible(formBottom0, false);
		} else if (max == formTop) {
			itemLeftTop.setImage(Img.MINIMIZE.get());
			itemRightTop.setImage(Img.RESTORE.get());
			itemLeftBottom0.setImage(Img.RESTORE.get());
			itemRightBottom0.setImage(Img.MAXIMIZE.get());
			setVisible(formTop0, false);
			setVisible(formBottom0, true);
		} else {
			itemLeftTop0.setImage(Img.RESTORE.get());
			itemRightTop0.setImage(Img.MAXIMIZE.get());
			itemLeftBottom.setImage(Img.MINIMIZE.get());
			itemRightBottom.setImage(Img.RESTORE.get());
			setVisible(formTop0, true);
			setVisible(formBottom0, false);
		}
		layout();
	}
	
	private void setVisible(@NotNull ToolBarFormHeader header, boolean isVisible) {
		GridData data = (GridData) header.getLayoutData();
		data.exclude = ! isVisible;
		header.setVisible(isVisible);
	}
	
	@NotNull
	protected Control createFirstContents(Composite parent) {
		return new Composite(parent, SWT.NONE);
	}
	
	@NotNull
	protected Control createSecondContents(Composite parent) {
		return new Composite(parent, SWT.NONE);
	}
	
	public final void setTopText(@NotNull String text) {
		formTop0.setText(text);
		formTop.setText(text);
	}
	
	public final void setBottomText(@NotNull String text) {
		formBottom0.setText(text);
		formBottom.setText(text);
	}
	
	public final void setTopImage(@Nullable Image image) {
		formTop0.setImage(image);
		formTop.setImage(image);
	}
	
	public final void setBottomImage(@Nullable Image image) {
		formBottom0.setImage(image);
		formBottom.setImage(image);
	}

}
