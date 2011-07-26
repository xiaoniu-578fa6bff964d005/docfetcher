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

import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class TreeCheckState {
	
	List<String> checkedPaths = new ArrayList<String>();
	int folderCount = 0;
	
	@NotNull
	public List<String> getCheckedPaths() {
		return checkedPaths;
	}
	
	public int getFolderCount() {
		return folderCount;
	}

}
