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

package net.sourceforge.docfetcher.util.gui;

import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * @author Tran Nam Quang
 */
public abstract class GroupWrapper {

	private final Group group;

	public GroupWrapper(@NotNull Composite parent, @NotNull String label) {
		group = new Group(parent, SWT.NONE);
		group.setText(label);
		createLayout(group);
		createContents(group);
	}
	
	@NotNull
	public final Group getGroup() {
		return group;
	}
	
	protected abstract void createLayout(@NotNull Group parent);
	
	protected abstract void createContents(@NotNull Group parent);
	
}
