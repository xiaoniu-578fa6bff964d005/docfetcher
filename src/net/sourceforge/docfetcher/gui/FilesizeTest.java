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

package net.sourceforge.docfetcher.gui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class FilesizeTest {
	
	@Test
	public void testConversion() {
		assertLongEquals(5L, FilesizeUnit.KB.convert(5600L, FilesizeUnit.Byte));
		assertLongEquals(0L, FilesizeUnit.GB.convert(1024000L, FilesizeUnit.Byte));
		assertLongEquals(286720L, FilesizeUnit.Byte.convert(280L, FilesizeUnit.KB));
		assertLongEquals(86973087744L, FilesizeUnit.Byte.convert(81L, FilesizeUnit.GB));
		assertLongEquals(450L, FilesizeUnit.MB.convert(450L, FilesizeUnit.MB));
	}
	
	private void assertLongEquals(long l1, long l2) {
		assertEquals(l1, l2);
	}

}
