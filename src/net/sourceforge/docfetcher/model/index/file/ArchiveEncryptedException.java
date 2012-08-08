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

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;

import net.sourceforge.docfetcher.util.annotations.Nullable;
import de.schlichtherle.truezip.file.TFile;

/**
 * @author Tran Nam Quang
 */
final class ArchiveEncryptedException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	private final File file;
	private final String originalPath;
	
	public ArchiveEncryptedException(@Nullable File file, @Nullable String originalPath) {
		/*
		 * For unknown reasons, serialization will fail with
		 * "NoClassDefFoundError: java/nio/file/Path" if we try to store TFile
		 * instances, so a regular file is stored instead.
		 */
		this.file = file instanceof TFile ? new File(file.getPath()) : file;
		this.originalPath = originalPath;
	}
	
	public File getFile() {
		return file;
	}
	
	public String getOriginalPath() {
		return originalPath;
	}

}
