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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A status bar widget consisting of multiple parts with the following
 * properties:
 * <ul>
 * <li>There is one left part and zero or more right parts.
 * <li>All parts have a text and an optional image.
 * <li>The right parts can be shown and hidden.
 * </ul>
 * This class is intended to be subclassed and that subclassers override
 * {@link #createRightParts(StatusBar)}.
 * 
 * @author Tran Nam Quang
 */
public class StatusBar extends Composite {
	
	/**
	 * Distance between the image and the text of a part.
	 */
	private static final int INNER_PART_SPACING = 5;
	
	/**
	 * Distance between parts.
	 */
	private static final int INTER_PART_SPACING = 20;

	/**
	 * A part of a status bar.
	 */
	public static final class StatusBarPart {
		@NotNull private StatusBar statusBar;
		@NotNull private Label imageLabel;
		@NotNull private Label textLabel;
		private boolean isVisible = true;
		
		public StatusBarPart(@NotNull StatusBar statusBar) {
			this.statusBar = statusBar;
			imageLabel = new Label(statusBar, SWT.NONE);
			textLabel = new Label(statusBar, SWT.NONE);
		}
		
		/**
		 * Returns whether this part is visible.
		 */
		public boolean isVisible() {
			return isVisible;
		}
		
		/**
		 * Sets the visibility of this part. Invisible parts are excluded from
		 * the layout. This method has no effect if the part is on the left side
		 * of the status bar.
		 */
		public void setVisible(boolean isVisible) {
			if (this.isVisible == isVisible) return;
			this.isVisible = isVisible;
			imageLabel.setVisible(isVisible);
			textLabel.setVisible(isVisible);
			statusBar.updateLayout();
		}
		
		/**
		 * Sets the image and text of this part. The image may be null.
		 */
		public void setContents(@Nullable Image image, @NotNull String text) {
			imageLabel.setImage(image);
			textLabel.setText(text);
			statusBar.updateLayout();
		}
		
		/**
		 * Returns the image displayed on this part, which may be null.
		 */
		@Nullable
		public Image getImage() {
			return imageLabel.getImage();
		}
		
		/**
		 * Returns the text displayed on this part.
		 */
		@NotNull
		public String getText() {
			return textLabel.getText();
		}
		
		/**
		 * Returns a rectangle describing the bounds of the receiver relative to
		 * containing shell, or null if the receiver is not visible.
		 */
		@Nullable
		public Rectangle getBounds() {
			if (!imageLabel.isVisible() || !textLabel.isVisible())
				return null;
			Rectangle b1 = imageLabel.getBounds();
			Rectangle b2 = textLabel.getBounds();
			final int s = INNER_PART_SPACING;
			int width = b1.width + b2.width	+ s;
			int height = Math.max(b1.height, b2.height);
			Rectangle bounds = new Rectangle(b1.x, b1.y, width, height);
			Shell shell = imageLabel.getShell();
			return shell.getDisplay().map(statusBar, shell, bounds);
		}
	}
	
	private StatusBarPart leftPart;
	private List<StatusBarPart> rightParts;
	
	/**
	 * Creates an instance with the given parent.
	 */
	public StatusBar(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FormLayout());
		leftPart = new StatusBarPart(this);
		rightParts = createRightParts(this);
		updateLayout();
	}
	
	/**
	 * Returns the left part.
	 */
	public final StatusBarPart getLeftPart() {
		return leftPart;
	}
	
	/**
	 * Creates and returns the parts to be added on the right side of the status
	 * bar.
	 */
	protected List<StatusBarPart> createRightParts(@NotNull StatusBar statusBar) {
		return new ArrayList<StatusBarPart> ();
	}
	
	/**
	 * Updates the layout of the status bar parts.
	 */
	private void updateLayout() {
		if (rightParts == null) return; // right parts are in the process of being created
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.margin(0).top().bottom();
		Label currentLabel = null;
		for (int i = rightParts.size() - 1; i >= 0; i--) {
			StatusBarPart part = rightParts.get(i);
			if (! part.isVisible) continue;
			if (currentLabel == null)
				fdf.right();
			else
				fdf.right(currentLabel, -INTER_PART_SPACING);
			fdf.applyTo(part.textLabel);
			fdf.right(part.textLabel, -INNER_PART_SPACING).applyTo(part.imageLabel);
			currentLabel = part.imageLabel;
		}
		
		fdf.reset().margin(0).top().bottom().left();
		if (leftPart.imageLabel.getImage() != null) {
			fdf.applyTo(leftPart.imageLabel);
			fdf.left(leftPart.imageLabel, INNER_PART_SPACING);
		}
		
		if (currentLabel == null)
			fdf.right();
		else
			fdf.right(currentLabel, -INTER_PART_SPACING);
		fdf.applyTo(leftPart.textLabel);
		
		layout();
	}

}
