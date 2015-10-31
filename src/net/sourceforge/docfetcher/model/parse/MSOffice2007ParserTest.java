package net.sourceforge.docfetcher.model.parse;

import java.io.File;

import junit.framework.Assert;
import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.junit.Test;

public final class MSOffice2007ParserTest {
	
	@Test
	public void testReadOnlySucceeds() throws Exception {
		/*
		 * In POI 3.10 FINAL, text extraction failed on this file when opened in
		 * read-only mode, but succeed in read-write mode. In POI 3.13, this bug
		 * has apparently been fixed.
		 */
		File file = TestFiles.read_write_xls.get();
		OPCPackage pkg = OPCPackage.open(file.getPath(), PackageAccess.READ);
		ExtractorFactory.createExtractor(pkg).getText();
	}
	
	@Test
	public void testReadWriteSucceeds() throws Exception {
		File file = TestFiles.read_write_xls.get();
		OPCPackage pkg = OPCPackage.open(file.getPath(), PackageAccess.READ_WRITE);
		ExtractorFactory.createExtractor(pkg).getText();
	}
	
	@Test
	public void testParse() throws Exception {
		File file = TestFiles.read_write_xls.get();
		long lengthBefore = file.length();
		long modBefore = file.lastModified();
		ParseService.parse(new IndexingConfig(), file, file.getName(),
			new Path(file), IndexingReporter.nullReporter, Cancelable.nullCancelable);
		Assert.assertEquals(lengthBefore, file.length());
		Assert.assertEquals(modBefore, file.lastModified());
	}

}
