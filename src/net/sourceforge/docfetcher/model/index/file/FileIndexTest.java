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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.base.ListMap;
import net.sourceforge.docfetcher.base.ListMap.Entry;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.model.NullCancelable;
import net.sourceforge.docfetcher.model.TreeNode;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

import org.apache.lucene.store.Directory;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class FileIndexTest {
	
	static {
		AppUtil.Const.autoInit();
	}
	
	@Test
	public void testNestedUpdate() throws Exception {
		String[] paths = {
				TestFiles.archive_zip_rar_7z,
				TestFiles.sfx_zip,
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
		File tempDir = Files.createTempDir();
		File htmlFile = new File(tempDir, "test.html");
		Files.copy(new File(TestFiles.html), htmlFile);
		File htmlDir = new File(tempDir, "test_files");
		File subFile1 = new File(htmlDir, "filename.unsupportedformat");
		File subFile2 = new File(htmlDir, "filename.txt");
		File subFile3 = new File(htmlDir, "simple.7z");
		htmlDir.mkdirs();
		subFile1.createNewFile();
		subFile2.createNewFile();
		Files.copy(new File(TestFiles.simple_7z), subFile3);
		
		IndexingConfig config = new IndexingConfig();
		FileIndex index = new FileIndex(config, null, tempDir);
		final CountingReporter reporter = new CountingReporter();
		
		// Index update should not detect any changes when nothing was modified
		index.update(reporter, NullCancelable.getInstance());
		assertEquals(1, reporter.counter);
		index.update(reporter, NullCancelable.getInstance());
		assertEquals(1, reporter.counter);
		
		// Index update must detect changes when files have been modified
		ListMap<File, Integer> fileMap = ListMap.<File, Integer> create()
			.add(htmlFile, 1)
			.add(subFile1, 0)
			.add(subFile2, 1)
			.add(subFile3, 1);
		int i = 0;
		for (ListMap.Entry<File, Integer> entry : fileMap) {
			reporter.counter = 0;
			
			File file = entry.getKey();
			int expectedCount = entry.getValue().intValue();
			file.setLastModified(System.currentTimeMillis() + (i + 1) * 1000);
			
			index.update(reporter, NullCancelable.getInstance());
			
			String msg = String.format("On '%s'.", file.getName());
			assertEquals(msg, expectedCount, reporter.counter);
			i++;
		}
		
		// Index update must detect change when HTML folder is deleted
		Files.deleteRecursively(htmlDir);
		reporter.counter = 0;
		index.update(reporter, NullCancelable.getInstance());
		assertEquals(1, reporter.counter);
		
		Files.deleteRecursively(tempDir);
	}
	
	@Test
	public void testHtmlPairUpdateInSevenZip() throws Exception {
		File testDir = new File(TestFiles.index_update_html_in_7z);
		List<File> files = Arrays.asList(Util.listFiles(testDir));
		Collections.sort(files);
		
		File originalFile = files.get(0);
		File tempDir = Files.createTempDir();
		File target = new File(tempDir, "target.7z");
		
		int[] expectedCounts = { 1, 1, 1, 1, 1, 0 };
		assertEquals(expectedCounts.length, files.size() - 1);
		
		for (boolean reversed : new boolean[] {false, true}) {
			int i = 0;
			for (File modifiedFile : files.subList(1, files.size())) {
				IndexingConfig config = new IndexingConfig();
				FileIndex index = new FileIndex(config, null, tempDir);
				
				File file1 = reversed ? modifiedFile : originalFile;
				File file2 = reversed ? originalFile : modifiedFile;
				
				Files.copy(file1, target);
				target.setLastModified(System.currentTimeMillis() - 1000);
				index.update(new IndexingReporter(), NullCancelable.getInstance());
				
				Files.copy(file2, target);
				CountingReporter reporter2 = new CountingReporter();
				
				index.update(reporter2, NullCancelable.getInstance());
				assertEquals(modifiedFile.getName(), expectedCounts[i], reporter2.counter);
				
				Files.deleteDirectoryContents(tempDir);
				i++;
			}
		}
		
		Files.deleteRecursively(tempDir);
	}
	
	private static class CountingReporter extends IndexingReporter {
		private int counter = 0;
		
		public void info(InfoType infoType, TreeNode treeNode) {
			if (infoType == InfoType.EXTRACTING)
				counter++;
		}
	}
	
	/**
	 * Checks that the index update works correctly after a folder is renamed.
	 */
	@Test
	public void testIndexUpdateAfterFolderRenaming() throws Exception {
		File tempDir = Files.createTempDir();
		
		// Set up subfolder
		File subDir1 = new File(tempDir, "Test1");
		subDir1.mkdir();
		File textFile = new File(subDir1, "test.txt");
		Files.write("Hello World", textFile, Charsets.UTF_8);
		
		// Create index
		IndexingConfig config = new IndexingConfig();
		FileIndex index = new FileIndex(config, null, tempDir);
		index.update(new IndexingReporter(), NullCancelable.getInstance());
		UtilModel.assertDocCount(index.getLuceneDir(), 1);
		
		// Rename subfolder, then update index
		File subDir2 = new File(tempDir, "Test2");
		subDir1.renameTo(subDir2);
		index.update(new IndexingReporter(), NullCancelable.getInstance());
		UtilModel.assertDocCount(index.getLuceneDir(), 1);
		
		Files.deleteRecursively(tempDir);
	}
	
	/**
	 * Checks that the index update works correctly after a file is renamed.
	 */
	@Test
	public void testIndexUpdateAfterFileRenaming() throws Exception {
		File tempDir = Files.createTempDir();
		
		File textFile = new File(tempDir, "test.txt");
		Files.write("Hello World", textFile, Charsets.UTF_8);
		
		IndexingConfig config = new IndexingConfig();
		FileIndex index = new FileIndex(config, null, tempDir);
		index.update(new IndexingReporter(), NullCancelable.getInstance());
		UtilModel.assertDocCount(index.getLuceneDir(), 1);
		
		textFile.renameTo(new File(tempDir, "test2.txt"));
		
		index.update(new IndexingReporter(), NullCancelable.getInstance());
		UtilModel.assertDocCount(index.getLuceneDir(), 1);
		
		Files.deleteRecursively(tempDir);
	}
	
	/**
	 * Checks that the index update works correctly after an archive entry
	 * (either a file or a folder) inside a 7z archive is renamed.
	 */
	@Test
	public void testIndexUpdateAfterRenamingIn7z() throws Exception {
		File tempDir = Files.createTempDir();
		
		ListMap<File, File> files = ListMap.create();
		files.add(new File(TestFiles.index_update_rename_in_7z, "file1.7z"),
			new File(TestFiles.index_update_rename_in_7z, "file2.7z"));
		files.add(new File(TestFiles.index_update_rename_in_7z, "folder1.7z"),
			new File(TestFiles.index_update_rename_in_7z, "folder2.7z"));
		
		for (Entry<File, File> entry : files) {
			File oldFile = entry.getKey();
			File newFile = entry.getValue();
			
			File target = new File(tempDir, "target.7z");
			Files.copy(oldFile, target);
			
			IndexingConfig config = new IndexingConfig();
			FileIndex index = new FileIndex(config, null, tempDir);
			index.update(new IndexingReporter(), NullCancelable.getInstance());
			UtilModel.assertDocCount(index.getLuceneDir(), 1);
			
			Files.copy(newFile, target);
			index.update(new IndexingReporter(), NullCancelable.getInstance());
			UtilModel.assertDocCount(index.getLuceneDir(), 1);
		}
		
		Files.deleteRecursively(tempDir);
	}
	
}
