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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.base.Util;

import org.junit.Test;

import SevenZip.Archive.SevenZipEntry;
import SevenZip.Archive.SevenZip.Handler;

import com.google.common.io.Closeables;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.rarfile.FileHeader;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileOutputStream;

/**
 * This unit test checks whether J7Zip and JUnRar, the two libraries used for
 * handling non-zip archives, can operate on compressed TrueZIP archive entries.
 * 
 * @author Tran Nam Quang
 */
public class TestUnpackFromZip {
	
	@Test
	public void testUnpackFromZip() {
		Checker[] checkers = {
				new RarChecker(),
				new SevenZipChecker(),
				// Instantiate tests here
		};
		for (Checker checker : checkers) {
			assertTrue(checker.entry.exists());
			try {
				checker.readUncompressed();
				assertTrue(true);
			} catch (Throwable t) {
			}
			try {
				checker.readCompressed();
				assertTrue(false);
			} catch (Throwable t) {
			}
		}
	}
	
	private static abstract class Checker {
		private TFile entry;
		public Checker(TestFiles testFile) {
			entry = new TFile(testFile.getPath());
		}
		public final void readUncompressed() throws Exception {
			TFile tempTFile = new TFile(Util.createTempFile("test", ""));
			entry.cp(tempTFile);
			read(tempTFile);
		}
		public final void readCompressed() throws Exception {
			read(entry);
		}
		protected abstract void read(TFile file) throws Exception;
	}
	
	private static class RarChecker extends Checker {
		public RarChecker() {
			super(TestFiles.truezip_compat_rar);
		}
		public void read(TFile file) throws Exception {
			Logger logger = Logger.getLogger(Archive.class.getName());
			logger.setLevel(Level.OFF);
			Archive archive = new Archive(file);
			FileHeader fh = archive.nextFileHeader();
			OutputStream out = new TFileOutputStream(Util.createTempFile(file.getName(), ""));
			archive.extractFile(fh, out);
			Closeables.closeQuietly(out);
			Closeables.closeQuietly(archive);
		}
	}
	
	private static class SevenZipChecker extends Checker {
		public SevenZipChecker() {
			super(TestFiles.truezip_compat_7z);
		}
		protected void read(TFile file) throws Exception {
			SevenZipInputStream istream = new SevenZipInputStream(file);
			Handler archive = new Handler();
			if (archive.Open(istream) != 0)
				throw new IOException();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < archive.size(); i++) {
				SevenZipEntry entry = archive.getEntry(i);
				sb.append(entry.getName()).append("\n");
			}
			archive.close();
		}
	}
	
}
