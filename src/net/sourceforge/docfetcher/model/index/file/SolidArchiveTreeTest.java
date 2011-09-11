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

package net.sourceforge.docfetcher.model.index.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.file.FileFolder.FileFolderVisitor;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class SolidArchiveTreeTest {
	
	static {
		AppUtil.Const.autoInit();
	}
	
	@Test
	public void testPreserveDirStructure() throws Exception  {
		IndexingConfig config = new IndexingConfig();
		List<SolidArchiveTree<?>> archives = Lists.newArrayList();
		try {
			File sevenZipFile = TestFiles.multiple_dirs_7z.get();
			archives.add(new SevenZipTree(sevenZipFile, config, null, null));

			File rarFile = TestFiles.multiple_dirs_rar.get();
			archives.add(new RarTree(rarFile, config, null, null));

			for (SolidArchiveTree<?> archive : archives) {
				final List<TreeNode> unpackList = new ArrayList<TreeNode> (1);
				new FileFolderVisitor <Exception> (archive.getArchiveFolder()) {
					protected void visitDocument(	FileFolder parent,
					                             	FileDocument fileDocument) {
						unpackList.add(fileDocument);
					}
				}.runSilently();
				
				assertEquals(1, unpackList.size());
				File tempDir = Files.createTempDir();
				archive.unpack(unpackList, tempDir);
				File unpackedFile = archive.getFile(unpackList.get(0));
				assertTrue(unpackedFile.isFile());
				
				String unpackedPath = Util.getAbsPath(unpackedFile);
				String tempDirPath = Util.getAbsPath(tempDir);
				String innerPath = "test1/test2/test3/test.txt";
				assertTrue(unpackedPath.equals(Util.joinPath(tempDirPath, innerPath)));
				
				Files.deleteRecursively(tempDir);
				assertFalse(unpackedFile.isFile());
			}
		} finally {
			for (SolidArchiveTree<?> archive : archives)
				Closeables.closeQuietly(archive);
		}
	}

}
