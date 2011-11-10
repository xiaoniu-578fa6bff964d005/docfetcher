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

package net.sourceforge.docfetcher.gui;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;
import net.sourceforge.docfetcher.util.collect.BoundedList;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public final class MultiFileLauncher {
	
	private final BoundedList<File> files = new BoundedList<File>(UtilGui.OPEN_LIMIT, false);
	private final Set<String> missing = new LinkedHashSet<String>();
	
	public void addFile(@NotNull File file) {
		Util.checkNotNull(file);
		if (!files.contains(file))
			files.add(file);
	}
	
	public void addMissing(@NotNull String path) {
		Util.checkNotNull(path);
		missing.add(path);
	}
	
	public void addMissing(@NotNull File file) {
		Util.checkNotNull(file);
		missing.add(Util.getSystemAbsPath(file));
	}
	
	// returns success
	public boolean launch() {
		// Abort with an error message if any files are missing
		if (!missing.isEmpty()) {
			String items = Util.join("\n", missing);
			String msg = Msg.files_or_folders_not_found.format(items);
			AppUtil.showError(msg, true, false);
			return false;
		}
		
		// Abort with an error message if the user tried to open too many files
		if (files.getVirtualSize() > files.getCapacity()) {
			String msg = Msg.open_limit.format(UtilGui.OPEN_LIMIT);
			AppUtil.showError(msg, true, true);
			return false;
		}
		
		// Open files or directories
		for (File file : files) {
			boolean success = Util.launch(file);
			if (!success) // This is to be expected for PST files
				Util.launch(Util.getParentFile(file));
		}
		
		return true;
	}

}
