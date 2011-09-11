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

package net.sourceforge.docfetcher.model.index;

import static org.junit.Assert.assertTrue;
import net.sourceforge.docfetcher.util.AppUtil;

import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class IndexingConfigTest {
	
	static {
		AppUtil.Const.autoInit();
	}
	
	@Test
	public void testCreateCustomTempFile() throws Exception {
		/*
		 * This test ensures that the filename of the created temp file has the
		 * same file extension as the input filename.
		 */
		IndexingConfig config = new IndexingConfig();
		String tempFilename = config.createDerivedTempFile("test.pdf").getName();
		assertTrue(tempFilename.startsWith("test"));
		assertTrue(tempFilename.endsWith(".pdf"));
	}

}
