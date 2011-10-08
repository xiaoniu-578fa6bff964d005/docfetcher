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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class TreeCheckState {
	
	List<Path> checkedPaths = new ArrayList<Path>();
	int folderCount = 0;
	
	@MutableCopy
	@NotNull
	public List<Path> getCheckedPaths() {
		return checkedPaths;
	}
	
	public int getFolderCount() {
		return folderCount;
	}

}
