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

import java.io.File;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.CallOnce;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
abstract class SimpleJNotifyListener {

	@Nullable private JNotifyListener listener = new JNotifyListener() {
		public final void fileCreated(int wd, String rootPath, String name) {
			handleEvent(new File(rootPath, name), false);
		}

		public final void fileDeleted(int wd, String rootPath, String name) {
			handleEvent(new File(rootPath, name), true);
		}

		public final void fileModified(int wd, String rootPath, String name) {
			handleEvent(new File(rootPath, name), false);
		}

		public final void fileRenamed(	int wd,
		                              	String rootPath,
		                              	String oldName,
		                              	String newName) {
			handleEvent(new File(rootPath, newName), false);
		}
	};
	
	protected abstract void handleEvent(@NotNull File targetFile,
										boolean deleted);
	
	@CallOnce
	public final int addWatch(@NotNull File watchFile) throws JNotifyException {
		Util.checkNotNull(listener, watchFile);
		String absPath = Util.getSystemAbsPath(watchFile);
		int id = JNotify.addWatch(absPath, JNotify.FILE_ANY, true, listener);
		listener = null; // ensure this method can only be called once
		return id;
	}

}
