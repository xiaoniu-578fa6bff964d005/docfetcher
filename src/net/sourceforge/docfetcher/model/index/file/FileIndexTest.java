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

import static org.junit.Assert.assertEquals;

import java.io.File;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.base.ListMap;
import net.sourceforge.docfetcher.model.NullCancelable;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

import org.apache.lucene.store.Directory;
import org.junit.Test;

import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class FileIndexTest {
	
	@Test
	public void testNestedUpdate() throws Exception {
		AppUtil.Const.autoInit();
		
		String[] paths = {
				TestFiles.archive_zip_rar_7z,
//				TestFiles.sfx_zip, // TODO won't work until ParseService supports zip archive entries
				TestFiles.sfx_7z,
				TestFiles.sfx_rar,
		};
		
		for (String path : paths) {
			IndexingConfig config = new IndexingConfig();
			File rootFile = new File(path);

			FileIndex index = new FileIndex(config, null, rootFile);
			index.update(new IndexingReporter(), NullCancelable.getInstance());
			Directory luceneDir = index.getLuceneDir();

			/*
			 * JUnRar doesn't support SFX rar archives, so we're expecting zero
			 * results for those.
			 */
			int expectedResultCount = path.equals(TestFiles.sfx_rar) ? 0 : 1;
			
			UtilModel.assertDocCount(luceneDir, expectedResultCount);
			UtilModel.assertResultCount(luceneDir, "test", expectedResultCount);
		}
	}
	
	/**
	 * Checks that the index update works correctly for HTML pairs.
	 */
	@Test
	public void testHtmlPairUpdate() throws Exception {
		AppUtil.Const.autoInit();
		
		File tempDir = Files.createTempDir();
		File htmlFile = new File(tempDir, "test.html");
		Files.copy(new File(TestFiles.html), htmlFile);
		File htmlDir = new File(tempDir, "test_files");
		File subFile1 = new File(htmlDir, "filename.unsupportedformat");
		File subFile2 = new File(htmlDir, "filename.txt");
		htmlDir.mkdirs();
		subFile1.createNewFile();
		subFile2.createNewFile();
		
		final int[] counter = { 0 };
		IndexingReporter reporter = new IndexingReporter() {
			public void info(InfoType infoType, TreeNode treeNode) {
				counter[0]++;
			}
		};
		
		IndexingConfig config = new IndexingConfig();
		FileIndex index = new FileIndex(config, null, tempDir);
		
		// Index update should not detect any changes when nothing was modified
		index.update(reporter, NullCancelable.getInstance());
		assertEquals(1, counter[0]);
		index.update(reporter, NullCancelable.getInstance());
		assertEquals(1, counter[0]);
		
		// Index update must detect changes when files have been modified
		ListMap<File, Integer> fileMap = ListMap.<File, Integer> create()
			.add(htmlFile, 1)
			.add(subFile1, 0)
			.add(subFile2, 1);
		for (ListMap.Entry<File, Integer> entry : fileMap) {
			counter[0] = 0;
			
			File file = entry.getKey();
			int expectedCount = entry.getValue().intValue();
			
			long lastModified = file.lastModified();
			while (lastModified == file.lastModified())
				Files.touch(file);
			
			index.update(reporter, NullCancelable.getInstance());
			
			String msg = String.format("On '%s'.", file.getName());
			assertEquals(msg, expectedCount, counter[0]);
		}
		
		// Index update must detect change when HTML folder is deleted
		Files.deleteRecursively(htmlDir);
		counter[0] = 0;
		index.update(reporter, NullCancelable.getInstance());
		assertEquals(1, counter[0]);
		
		Files.deleteRecursively(tempDir);
	}
	
	// TODO test: add more tests

}
