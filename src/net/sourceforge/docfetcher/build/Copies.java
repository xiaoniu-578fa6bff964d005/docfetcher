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

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;

/**
 * @author Tran Nam Quang
 */
final class Copies {
	
	private Copy copy = new Copy();
	
	public Copies() {
		copy.setProject(new Project()); // copy will fail without this
	}
	
	public Copies addDir(String dir) {
		copy.addFileset(new FileSets().setDir(dir).get());
		return this;
	}
	
	public Copies addFile(String file) {
		copy.addFileset(new FileSets().setFile(file).get());
		return this;
	}
	
	public Copies addDir(	@NotNull String dir,
							@Nullable String include,
							@Nullable String exclude) {
		FileSet fileSet = new FileSets().setDir(dir)
				.include(include)
				.exclude(exclude)
				.get();
		copy.addFileset(fileSet);
		return this;
	}
	
	public Copies setTodir(String dstDir) {
		copy.setTodir(new File(dstDir));
		return this;
	}
	
	public Copies setTofile(String dstFile) {
		copy.setTofile(new File(dstFile));
		return this;
	}
	
	public Copies flatten() {
		copy.setFlatten(true);
		return this;
	}
	
	public void execute() {
		copy.execute();
	}

}
