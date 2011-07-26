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

import net.sourceforge.docfetcher.base.annotations.Nullable;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * @author Tran Nam Quang
 */
final class FileSets {
	
	private FileSet fileSet = new FileSet();
	
	public FileSets() {
		fileSet.setProject(new Project());
	}
	
	public FileSets setDir(String dir) {
		fileSet.setDir(new File(dir));
		return this;
	}
	
	public FileSets setFile(String file) {
		fileSet.setFile(new File(file));
		return this;
	}
	
	public FileSets include(@Nullable String includes) {
		if (includes != null)
			fileSet.setIncludes(includes);
		return this;
	}
	
	public FileSets exclude(@Nullable String excludes) {
		if (excludes != null)
			fileSet.setExcludes(excludes);
		return this;
	}
	
	public FileSet get() {
		return fileSet;
	}

}
