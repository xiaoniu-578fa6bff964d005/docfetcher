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

package net.sourceforge.docfetcher.model;

import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * A node of a tree structure that can be displayed on the user interface. (This
 * contrasts with nodes that only serve internal purposes.)
 * 
 * @author Tran Nam Quang
 */
public interface ViewNode {

	/**
	 * Returns a label for displaying the receiver on the user interface. The
	 * returned label is not guaranteed to be unique.
	 */
	@NotNull
	public String getDisplayName();
	
	@NotNull
	public boolean isChecked();
	
	@NotNull
	public void setChecked(boolean isChecked);
	
	@NotNull
	public Iterable<ViewNode> getChildren();
	
	public boolean isIndex();

}