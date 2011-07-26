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

package net.sourceforge.docfetcher.base;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public class UtilTest {
	
	@Test
	public void testSplitPath() {
		String[][] samples = {
				// input path, expected path part outputs
				{"path/to/file", "path", "to", "file"},
				{"/path/to/file", "", "path", "to", "file"},
				{"path/to/file/", "path", "to", "file"},
				{"/path/to/file/", "", "path", "to", "file"},
				
				{"/", ""},
				{"//", "", ""},
				{"", ""},
		};
		for (String[] sample : samples) {
			List<String> expected = new ArrayList<String> ();
			for (int i = 1; i < sample.length; i++)
				expected.add(sample[i]);
			List<String> actual = Util.splitPath(sample[0]);
			if (expected.size() != 0 && actual.size() != 0)
				assertEquals(expected, actual);
		}
	}
	
	@Test
	public void testJoinPath() {
		String[][] samples = {
				// expected path output, parts to join
				{"", ""},
				{"path/to/file", "path", "to", "file"},
				{"path/to/file", "path/", "to", "file/"},
				{"path", "path"},
		};
		for (String[] sample : samples) {
			String expected = sample[0];
			String actual = Util.joinPath(Arrays.copyOfRange(sample, 1, sample.length));
			assertEquals(expected, actual);
		}
	}
	
	@Test
	public void testCreateTempFile() throws IOException {
		File file = Util.createTempFile("1", null);
		assertTrue(file.exists());
		assertTrue(file.getName().startsWith("1__"));
	}
	
	@Test
	public void testSplitPathLast() {
		List<Sample> samples = new ArrayList<Sample> ();
		add(samples, "", "", "");
		add(samples, "/", "", "");
		add(samples, "path/to/file", "path/to", "file");
		add(samples, "path/to/file/", "path/to/file", "");
		add(samples, "path-to-file", "path-to-file", "");
		add(samples, "/path-to-file", "", "path-to-file");
		
		for (Sample sample : samples) {
			String[] actualOutput = Util.splitPathLast(sample.input);
			assertArrayEquals(sample.expectedOutput, actualOutput);
		}
	}
	
	private static void add(List<Sample> samples, String input, String... expectedOutput) {
		Sample sample = new Sample();
		sample.input = input;
		sample.expectedOutput = expectedOutput;
		samples.add(sample);
	}
	
	private static class Sample {
		String input;
		String[] expectedOutput;
	}

}
