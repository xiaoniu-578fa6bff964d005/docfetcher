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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.sourceforge.docfetcher.model.index.file.FileDocument;
import net.sourceforge.docfetcher.model.index.file.FileFolder;

import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class FolderTest {
	
	@Test
	public void testFindDocument() {
		for (boolean absolutePath : new boolean[] { true, false }) {
			String prefix = absolutePath ? "/" : "";
			
			FileFolder f1 = new FileFolder(new Path(prefix + "one"), null);
			FileFolder f2 = new FileFolder(new Path(prefix + "one/two"), null);
			f1.putSubFolder(f2);
			FileDocument doc = new FileDocument(f2, "three", 1L);
			
			String targetPath = prefix + "one/two/three";
			assertTrue(doc == f1.findTreeNode(new Path(targetPath)));
			
			String wrongPrefix = absolutePath ? "" : "/";
			String wrongPath = wrongPrefix + "one/two/three";
			assertNull(f1.findTreeNode(new Path(wrongPath)));
			
			assertNull(f1.findTreeNode(new Path(prefix + "does/not/exist")));
		}
	}

}
