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

package net.sourceforge.docfetcher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
				{"", "", ""},
				{"path", "path", ""},
				{"path", "", "path"},
				{"path/to/file", "path", "to", "file"},
				{"path/to/file", "path/", "to", "file/"},
		};
		for (String[] sample : samples) {
			String expected = sample[0];
			String[] moreParts = Arrays.copyOfRange(sample, 3, sample.length);
			String actual = Util.joinPath(sample[1], sample[2], moreParts);
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
	public void testIsDriveLetter() {
		class Sample {
			private final String input;
			private final boolean expectedOutput;
			private Sample(String input, boolean expectedOutput) {
				this.input = input;
				this.expectedOutput = expectedOutput;
			}
		}
		Sample[] samples = {
			new Sample("C:", true),
			new Sample("G:\\", true),
			new Sample("x:\\\\", true),
			new Sample("f:\\Foo\\", false),
			new Sample("c:\\\\Foo", false),
			new Sample("c://", true),
			new Sample("c://Foo", false),
			new Sample("c:\\/", true),
		};
		for (Sample sample : samples) {
			boolean output = Util.isWindowsDevice(sample.input);
			assertEquals(sample.expectedOutput, output);
		}
	}
	
	@Test
	public void testGetCanonicalFile() {
		if (Util.IS_WINDOWS) {
			File file = Util.getCanonicalFile("C:\\//\\");
			assertEquals("C:", file.getPath());
		}
	}
	
}
