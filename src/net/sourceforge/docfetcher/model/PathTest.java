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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.model.Path.PathParts;

import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class PathTest {
	
	@Test
	public void testSplitPathLast() {
		List<Sample> samples = new ArrayList<Sample> ();
		add(samples, "", "", "");
		add(samples, "/", "", "");
		add(samples, "path/to/file", "path/to", "file");
		add(samples, "path/to/file/", "path/to", "file");
		add(samples, "path-to-file", "path-to-file", "");
		add(samples, "/path-to-file", "", "path-to-file");
		
		for (Sample sample : samples) {
			PathParts actualOutput = new Path(sample.input).splitAtLastSeparator();
			assertEquals(sample.expectedOutput[0], actualOutput.getLeft().getPath());
			assertEquals(sample.expectedOutput[1], actualOutput.getRight());
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
