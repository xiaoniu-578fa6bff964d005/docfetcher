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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.model.FileResource;
import net.sourceforge.docfetcher.model.HotColdFileCache;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.parse.ParseException;

import org.junit.Test;

import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class FileFactoryTest {
	
	static {
		AppUtil.Const.autoInit();
	}
	
	@Test
	public void testUnpack() throws Exception {
		String testFile = TestFiles.archive_entry_7z_zip_rar.getPath();
		String[] paths = {
				testFile,
				"./" + testFile,
				"../" + Util.USER_DIR.getName() + "/" + testFile,
				Util.getAbsPath(testFile),
				TestFiles.archive_entry_zip_zip.getPath()
		};
		IndexingConfig config = new IndexingConfig();
		for (String path : paths) {
			HotColdFileCache unpackCache = new HotColdFileCache(20);
			FileFactory fileFactory = new FileFactory(unpackCache);
			File file = fileFactory.createFile(config, path).getFile();
			assertTrue(file.exists());
			file.delete();
		}
	}
	
	@Test
	public void testUnpackCache() throws Exception {
		/*
		 * This method tests the correctness of the caching mechanism: Calling
		 * the fileFactory multiple times with the same argument must yield exactly
		 * the same file.
		 */
		String testFile = TestFiles.archive_entry_7z_zip_rar.getPath();
		Set<File> files = new HashSet<File> ();
		HotColdFileCache unpackCache = new HotColdFileCache(20);
		FileFactory fileFactory = new FileFactory(unpackCache);
		IndexingConfig config = new IndexingConfig();
		for (int i = 0; i < 5; i++) {
			files.add(fileFactory.createFile(config, testFile).getFile());
			String absPath = Util.getAbsPath(testFile);
			files.add(fileFactory.createFile(config, absPath).getFile());
		}
		assertEquals(1, files.size());
	}
	
	@Test
	public void testUnpackCacheLimit() throws Exception {
		final int cacheSize = 20;
		HotColdFileCache unpackCache = new HotColdFileCache(cacheSize);
		FileFactory fileFactory = new FileFactory(unpackCache);
		IndexingConfig config = new IndexingConfig();
		File archive = TestFiles.simple_7z.get();
		for (int i = 0; i < cacheSize * 2; i++) {
			File archiveCopy = Util.createDerivedTempFile("test.7z", Util.TEMP_DIR);
			Files.copy(archive, archiveCopy);
			String path = Util.joinPath(Util.getAbsPath(archiveCopy), "test.txt");
			FileResource fileResource = fileFactory.createFile(config, path);
			File tempFile = fileResource.getFile();
			assertTrue(tempFile.isFile());
			fileResource.dispose();
			int actualCacheSize = unpackCache.getActualCacheSize();
			assertTrue(0 < actualCacheSize);
			assertTrue(actualCacheSize <= cacheSize);
		}
	}
	
	@Test
	public void testUnpackSfxArchives() throws Exception {
		HotColdFileCache unpackCache = new HotColdFileCache(20);
		FileFactory fileFactory = new FileFactory(unpackCache);
		IndexingConfig config = new IndexingConfig();
		String[] paths = {
				TestFiles.sfx_zip.getPath() + "/test.txt",
				TestFiles.sfx_7z.getPath() + "/test.txt"
		};
		for (String path : paths)
			assertTrue(fileFactory.createFile(config, path).getFile().isFile());
	}
	
	@Test(expected=ParseException.class)
	public void testUnpackSfxRarArchive() throws Exception {
		HotColdFileCache unpackCache = new HotColdFileCache(20);
		FileFactory fileFactory = new FileFactory(unpackCache);
		IndexingConfig config = new IndexingConfig();
		fileFactory.createFile(config, TestFiles.sfx_rar + "/test.txt");
	}

}
