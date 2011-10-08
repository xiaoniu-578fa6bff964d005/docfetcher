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
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.CallOnce;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
abstract class SimpleJNotifyListener {
	
	enum EventType {
		CREATED, DELETED, MODIFIED, RENAMED
	}

	@Nullable private JNotifyListener listener = new JNotifyListener() {
		public final void fileCreated(int wd, String rootPath, String name) {
			handleEvent(rootPath, name, EventType.CREATED);
		}

		public final void fileDeleted(int wd, String rootPath, String name) {
			handleEvent(rootPath, name, EventType.DELETED);
		}

		public final void fileModified(int wd, String rootPath, String name) {
			handleEvent(rootPath, name, EventType.MODIFIED);
		}

		public final void fileRenamed(	int wd,
		                              	String rootPath,
		                              	String oldName,
		                              	String newName) {
			handleEvent(rootPath, newName, EventType.RENAMED);
		}
	};
	
	private void handleEvent(	@NotNull String rootPath,
								@NotNull String name,
								@NotNull EventType eventType) {
		// Calling File.getAbsoluteFile() is probably not necessary, but just in case...
		handleEvent(new File(rootPath, name).getAbsoluteFile(), eventType);
	}
	
	// The given file is always absolute
	protected abstract void handleEvent(@NotNull File targetFile,
										@NotNull EventType eventType);
	
	@CallOnce
	public final int addWatch(@NotNull File watchFile) throws JNotifyException {
		Util.checkNotNull(listener, watchFile);
		String absPath = Util.getSystemAbsPath(watchFile);
		int id = JNotify.addWatch(absPath, JNotify.FILE_ANY, true, listener);
		listener = null; // ensure this method can only be called once
		return id;
	}

}
