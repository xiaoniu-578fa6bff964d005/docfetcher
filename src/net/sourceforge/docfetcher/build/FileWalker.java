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

package net.sourceforge.docfetcher.build;

import java.io.File;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
abstract class FileWalker {
	
	public final void run(@NotNull File root) {
		for (File file : Util.listFiles(root)) {
			if (file.isFile()) {
				handleFile(file);
			}
			else {
				handleDir(file);
				run(file);
			}
		}
	}
	
	protected void handleFile(@NotNull File file) {}
	
	protected void handleDir(@NotNull File dir) {}

}
