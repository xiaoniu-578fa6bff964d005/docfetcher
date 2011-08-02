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

package net.sourceforge.docfetcher.model.index.outlook;

import java.io.File;

import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.model.NullCancelable;
import net.sourceforge.docfetcher.model.UtilModel;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

import org.apache.lucene.store.Directory;
import org.junit.Test;

/**
 * @author Tran Nam Quang
 */
public final class OutlookIndexTest {

	static {
		AppUtil.Const.autoInit();
	}
	
	@Test
	public void testSimple() throws Exception {
		IndexingConfig config = new IndexingConfig();
		File pstFile = TestFiles.outlook_test.get();
		
		OutlookIndex index = new OutlookIndex(config, null, pstFile);
		index.update(new IndexingReporter(), NullCancelable.getInstance());
		Directory luceneDir = index.getLuceneDir();
		
		UtilModel.assertDocCount(luceneDir, 1);
		UtilModel.assertResultCount(luceneDir, "Subject1", 1);
		UtilModel.assertResultCount(luceneDir, "Body1", 1);
		
		UtilModel.assertResultCount(luceneDir, "test", 1);
		UtilModel.assertResultCount(luceneDir, "\"test.pdf\"", 1);
	}
	
	// TODO test
	
}
