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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.docfetcher.model.index.file.FileFolder;

import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class TreeCheckStateTest {
	
	@Test
	public void testCommonCase() {
		FileFolder root = new FileFolder(new Path("../../Root"), null);
		FileFolder f1 = new FileFolder(root, "Folder1", null);
		FileFolder f2 = new FileFolder(root, "Folder2", null);
		FileFolder sf = new FileFolder(f1, "SubFolder", null);
		
		root.setChecked(true);
		f1.setChecked(false);
		f2.setChecked(true);
		sf.setChecked(false);
		
		TreeCheckState checkState = root.getTreeCheckState();
		List<FileFolder> folders = Arrays.asList(root, f1, f2, sf);
		for (FileFolder f : folders) {
			boolean actualOutput = checkState.isChecked(f.getPath());
			assertEquals(f.isChecked(), actualOutput);
		}
	}

}
