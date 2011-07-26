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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.Path;

/**
 * @author Tran Nam Quang
 */
final class Paths {
	
	private final Path path = new Path(new Project());
	
	public Paths addFileSet(String dir, String includes) {
		path.addFileset(new FileSets().setDir(dir).include(includes).get());
		return this;
	}
	
	public Paths addDirSet(String dirSet) {
		DirSet _dirSet = new DirSet();
		_dirSet.setFile(new File(dirSet));
		path.addDirset(_dirSet);
		return this;
	}
	
	public Path get() {
		return path;
	}

}
