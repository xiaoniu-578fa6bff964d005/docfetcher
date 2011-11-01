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

package net.sourceforge.docfetcher.util.gui;

import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Sash;

/**
 * A sash form similar to the SWT {@link org.eclipse.swt.custom.SashForm
 * SashForm}, but with the following differences:
 * <ul>
 * <li>This sash form can only manage two controls, referred to as 'first
 * control' and 'second control'.
 * <li>The first control has a fixed dimension (either with or height),
 * depending on the orientation of the sash form. A fixed width/height means the
 * width/height won't change when the separating sash is dragged, or when the
 * sash form is resized as a whole. The second control on the other hand will be
 * resized to fill the available space.
 * <li>The two controls are provided by subclassers who must implement
 * {@link #createFirstControl} and {@link #createSecondControl}.
 * </ul>
 * 
 * @author Tran Nam Quang
 */
public abstract class FixedSashForm extends Composite {
	
	public final Event<Boolean> evtFirstControlShown = new Event<Boolean>();

	private int limit = 50;
	private Sash sash;
	private Control firstControl;
	private Control secondControl;
	private boolean isHorizontal;
	private boolean isFirstFixed;
	private boolean isFirstVisible = true;
	private boolean isSmooth = false;

	/**
	 * Creates an instance with the given parent and orientation, and an initial
	 * value for the fixed dimension of the first control.
	 * <p>
	 * The orientation can be SWT.TOP, SWT.BOTTOM, SWT.LEFT or SWT.RIGHT. If the
	 * orientation is SWT.TOP or SWT.BOTTOM, the height of the first control is
	 * fixed, otherwise its width is fixed.
	 */
	public FixedSashForm(Composite parent, int orientation, int startOffset) {
		super(parent, SWT.NONE);
		boolean isLeft = Util.contains(orientation, SWT.LEFT);
		isHorizontal = isLeft || Util.contains(orientation, SWT.RIGHT);
		isFirstFixed = Util.contains(orientation, SWT.TOP) || isLeft;
		
		setLayout(new FormLayout());
		firstControl = createFirstControl(this);
		sash = new Sash(this, isHorizontal ? SWT.VERTICAL : SWT.HORIZONTAL);
		secondControl = createSecondControl(this);

		sash.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (! isSmooth && e.detail == SWT.DRAG)
					return;
				setFixedDimension(isHorizontal ? e.x : e.y);
			}
		});

		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).left().top();
		if (isHorizontal) {
			fdf.bottom().right(sash).applyTo(firstControl);
			fdf.reset().margin(0).top().bottom();
			if (isFirstFixed)
				fdf.left(0, startOffset);
			else
				fdf.right(100, -startOffset);
			fdf.applyTo(sash);
			fdf.left(sash).right().applyTo(secondControl);
		} else {
			fdf.bottom(sash).right().applyTo(firstControl);
			fdf.reset().margin(0).left().right();
			if (isFirstFixed)
				fdf.top(0, startOffset);
			else
				fdf.bottom(100, -startOffset);
			fdf.applyTo(sash);
			fdf.top(sash).bottom().applyTo(secondControl);
		}
	}
	
	/**
	 * Returns the width of the sash.
	 */
	public final int getSashWidth() {
		return ((FormData) sash.getLayoutData()).width;
	}
	
	/**
	 * Sets the width of the sash.
	 */
	public final void setSashWidth(int width) {
		((FormData) sash.getLayoutData()).width = width;
		layout(false);
	}

	/**
	 * Returns the minimum for the fixed dimension of the first control.
	 */
	public final int getLimit() {
		return limit;
	}

	/**
	 * Sets the minimum for the fixed dimension of the first control.
	 */
	public final void setLimit(int limit) {
		this.limit = limit;
	}
	
	/**
	 * Returns whether the first control is visible.
	 */
	public final boolean isFirstControlVisible() {
		return isFirstVisible;
	}
	
	/**
	 * Sets the visibility of the first control. If it's invisible, the second
	 * control will fill the entire sash form.
	 */
	public final void setFirstControlVisible(boolean isVisible) {
		if (this.isFirstVisible == isVisible) return;
		this.isFirstVisible = isVisible;
		firstControl.setVisible(isVisible);
		sash.setVisible(isVisible);
		if (isVisible) {
			setLayout(new FormLayout());
		} else {
			StackLayout stackLayout = new StackLayout();
			stackLayout.topControl = secondControl;
			setLayout(stackLayout);
		}
		layout();
		evtFirstControlShown.fire(isVisible);
	}
	
	/**
	 * Returns the fixed dimension of the first control.
	 */
	public final int getFixedDimension() {
		Control fixedControl = isFirstFixed ? firstControl : secondControl;
		Point size = fixedControl.getSize();
		return isHorizontal ? size.x : size.y;
	}
	
	public final void setFixedDimension(int newOffset) {
		FixedSashForm parent = FixedSashForm.this;
		Rectangle parentRect = parent.getClientArea();
		Rectangle sashRect = sash.getBounds();
		FormData sashData = (FormData) sash.getLayoutData();
		
		int lower = parent.limit;
		int upper = -1;
		if (isHorizontal)
			upper = parentRect.width - sashRect.width - lower;
		else
			upper = parentRect.height - sashRect.height - lower;
		newOffset = Math.max(Math.min(newOffset, upper), lower);
		
		int oldOffset = isHorizontal ? sashRect.x : sashRect.y;
		if (newOffset == oldOffset) return;
		
		if (isHorizontal) {
			if (isFirstFixed)
				sashData.left = new FormAttachment(0, newOffset);
			else
				sashData.right = new FormAttachment(100,
						- parentRect.width + sashRect.width + newOffset);
		} else {
			if (isFirstFixed)
				sashData.top = new FormAttachment(0, newOffset);
			else
				sashData.bottom = new FormAttachment(100,
						- parentRect.height + sashRect.height + newOffset);
		}
		parent.layout();
	}
	
	/**
	 * Returns whether 'smooth' dragging for the sash is enabled.
	 */
	public final boolean isSmooth() {
		return isSmooth;
	}
	
	/**
	 * Enables or disables 'smooth' dragging for the sash.
	 */
	public final void setSmooth(boolean isSmooth) {
		this.isSmooth = isSmooth;
	}

	/**
	 * Creates and returns the first control with the given parent.
	 */
	protected abstract Control createFirstControl(Composite parent);

	/**
	 * Creates and returns the second control with the given parent.
	 */
	protected abstract Control createSecondControl(Composite parent);

	/**
	 * Returns the first control.
	 */
	public final Control getFirstControl() {
		return firstControl;
	}

	/**
	 * Returns the second control.
	 */
	public final Control getSecondControl() {
		return secondControl;
	}

}
