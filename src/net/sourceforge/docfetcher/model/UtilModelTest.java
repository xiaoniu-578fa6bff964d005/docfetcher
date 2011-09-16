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
import static org.junit.Assert.assertTrue;

import net.sourceforge.docfetcher.TestFiles;
import org.junit.Test;

import de.schlichtherle.truezip.file.TFile;

/**
 * @author Tran Nam Quang
 */
public final class UtilModelTest {
	
	@Test
	public void testGetRelativePath() {
		String[] tests = {
				// src -> dst -> expected output
				
				// case: equality
				"/path/to/file", "/path/to/file", "/path/to/file",
				"", "", "",
				"C:", "C:\\", "C:",
				"/same/path", "same/path/", "../../../same/path",
				
				// case: src above dst
				"/path/to/file", "/path/to/file/and/more", "and/more",
				"", "some/path", "some/path",
				
				// case: src below dst
				"/path/to/file/and/more", "/path/to/file", "../..",
				"C:\\Windows\\System\\Temp\\", "C:\\Windows\\", "../..",
				
				// case: src and dst beside each other
				"/path/to/one/file", "/path/to/another/file", "../../another/file",
				"C:\\Windows\\System", "C:\\Program Files\\DocFetcher", "../../Program Files/DocFetcher",
				"path/to/file", "some/thing/else", "../../../some/thing/else",
				"C:", "D:\\Windows\\", "../D:/Windows",
		};
		for (int i = 0; i < tests.length - 2; i += 3) {
			String src = tests[i];
			String dst = tests[i + 1];
			String outExpected = tests[i + 2];
			String outActual = UtilModel.getRelativePath(src, dst);
			assertEquals(outExpected, outActual);
		}
	}
	
	@Test
	public void testArchiveDetection() {
		String parentPath = TestFiles.archive_detection.getPath();
		String[] candidateNames = {
			"real-zip.zip",
			"fake-zip-is-file.zip",
			"fake-zip-is-directory.zip",
			"nested-test.zip/real-zip.zip",
			"nested-test.zip/fake-zip-is-file.zip",
			"nested-test.zip/fake-zip-is-directory.zip"
		};
		boolean[] expectedOutputs = {
			true, false, false,
			true, false, false
		};
		assertTrue(candidateNames.length == expectedOutputs.length);
		for (int i = 0; i < candidateNames.length; i++) {
			String fullPath = parentPath + "/" + candidateNames[i];
			TFile file = new TFile(fullPath);
			assertTrue(file.exists());
			boolean actualOutput = UtilModel.isZipArchive(file);
			assertEquals(expectedOutputs[i], actualOutput);
		}
	}
	
}
