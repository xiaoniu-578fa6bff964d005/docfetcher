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

import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.FixedSashForm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A three-panel layout with one panel on the left, two panels on the right, and
 * layout toggle buttons in between. The right-hand panels can be either on top
 * of or next to each other, and the second right-hand panel can be hidden.
 * <p>
 * These three panels are to be created by subclassers by implementing
 * {@link #createFirstControl}, {@link #createFirstSubControl} and
 * {@link #createSecondSubControl}.
 * 
 * @author Tran Nam Quang
 */
public abstract class ThreePanelForm extends FixedSashForm {
	
	// TODO now: Vertical and horizontal layout have separate sash weights
	
	private SashForm sash;
	private Composite secondControl;
	private Composite firstSubControlWrapper;
	private ThinArrowButton leftBt;
	private ThinArrowButton bottomBt;
	private ThinArrowButton outerRightBt;
	private ThinArrowButton innerRightBt;
	
	/**
	 * Creates an instance with the given parent and the given initial width for
	 * the left panel.
	 */
	public ThreePanelForm(Composite parent, int startOffset) {
		super(parent, SWT.LEFT, startOffset);
	}
	
	/**
	 * Creates and returns the first right-hand panel with the given parent.
	 */
	@NotNull
	protected abstract Control createFirstSubControl(Composite parent);
	
	/**
	 * Creates and returns the second right-hand panel with the given parent.
	 */
	@NotNull
	protected abstract Control createSecondSubControl(Composite parent);
	
	protected final Control createSecondControl(Composite parent) {
		secondControl = new Composite(parent, SWT.NONE);
		secondControl.setLayout(Util.createGridLayout(3, false, 0, 0));
		
		leftBt = new ThinArrowButton(secondControl, SWT.LEFT);
		leftBt.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		sash = new SashForm(secondControl, SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		outerRightBt = new ThinArrowButton(secondControl, SWT.LEFT);
		outerRightBt.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 2));
		
		firstSubControlWrapper = new Composite(sash, SWT.NONE);
		firstSubControlWrapper.setLayout(Util.createGridLayout(2, false, 0, 0));

		createFirstSubControl(firstSubControlWrapper)
		.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		innerRightBt = new ThinArrowButton(firstSubControlWrapper, SWT.RIGHT);
		innerRightBt.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 2));

		bottomBt = new ThinArrowButton(firstSubControlWrapper, SWT.UP);
		bottomBt.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, false, false));

		createSecondSubControl(sash);
		
		leftBt.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				boolean isVisible = ! isFirstControlVisible();
				setFirstControlVisible(isVisible);
			}
		});
		
		/*
		 * Update the orientation of the left button when it's clicked or when
		 * the left panel is shown or hidden programmatically.
		 */
		evtFirstControlShown.add(new Event.Listener<Boolean>() {
			public void update(Boolean eventData) {
				leftBt.setOrientation(eventData ? SWT.LEFT : SWT.RIGHT);
			}
		});
		
		MouseAdapter rightBtHandler = new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				if (isSecondSubControlVisible() && ! isVertical()) {
					setSecondSubControlVisible(false);
				} else {
					setSecondSubControlVisible(true);
					setVertical(false);
				}
			}
		};
		outerRightBt.addMouseListener(rightBtHandler);
		innerRightBt.addMouseListener(rightBtHandler);
		
		bottomBt.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				if (isSecondSubControlVisible() && isVertical()) {
					setSecondSubControlVisible(false);
				} else {
					setSecondSubControlVisible(true);
					setVertical(true);
				}
			}
		});
		
		updateRightButtons();
		return secondControl;
	}
	
	private void updateRightButtons() {
		boolean isBottomVisible = isSecondSubControlVisible() && isVertical();
		boolean isRightVisible = isSecondSubControlVisible() && ! isVertical();
		
		outerRightBt.setOrientation(isRightVisible ? SWT.RIGHT : SWT.LEFT);
		innerRightBt.setOrientation(isRightVisible ? SWT.RIGHT : SWT.LEFT);
		bottomBt.setOrientation(isBottomVisible ? SWT.DOWN : SWT.UP);
		
		outerRightBt.setVisible(isBottomVisible);
		((GridData) outerRightBt.getLayoutData()).exclude = ! isBottomVisible;
		((GridLayout) secondControl.getLayout()).numColumns = isBottomVisible ? 3 : 2;

		innerRightBt.setVisible(! isBottomVisible);
		((GridData) innerRightBt.getLayoutData()).exclude = isBottomVisible;
		((GridLayout) firstSubControlWrapper.getLayout()).numColumns = isBottomVisible ? 1 : 2;
		
		secondControl.layout();
	}
	
	/**
	 * Returns the width of the secondary sash.
	 */
	public final int getSubSashWidth() {
		return sash.getSashWidth();
	}
	
	/**
	 * Sets the width of the secondary sash.
	 */
	public final void setSubSashWidth(int width) {
		sash.setSashWidth(width);
	}
	
	/**
	 * Returns whether the second subcontrol is visible.
	 */
	public final boolean isSecondSubControlVisible() {
		return sash.getMaximizedControl() == null;
	}
	
	/**
	 * Sets the visibility of the second subcontrol.
	 */
	public final void setSecondSubControlVisible(boolean isVisible) {
		if (isVisible)
			sash.setMaximizedControl(null);
		else
			sash.setMaximizedControl(firstSubControlWrapper);
		updateRightButtons();
	}
	
	/**
	 * Returns true if the orientation of the sash separating the two
	 * subcontrols is SWT.VERTICAL, otherwise false.
	 */
	public final boolean isVertical() {
		return sash.getOrientation() == SWT.VERTICAL;
	}

	/**
	 * Sets the orientation of the sash separating the two subcontrols.
	 */
	public final void setVertical(boolean isVertical) {
		sash.setOrientation(isVertical ? SWT.VERTICAL : SWT.HORIZONTAL);
		updateRightButtons();
	}

}
