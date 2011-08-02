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

package net.sourceforge.docfetcher;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class TestFiles {
	
	private TestFiles() {
	}
	
	@NotNull
	private static String create(@NotNull String path) {
		return Util.joinPath("dev/test-files", path);
	}
	
	public static final String archive_entry_7z_zip_rar =
		create("zip-rar.7z/"
		+ "test.7z/archive.zip/"
		+ "test.zip/archive.rar/"
		+ "test.rar/test2.txt");
	
	public static final String archive_entry_zip_zip =
		create("zip.zip/test.zip/test.txt");
	
	public static final String multi_page_pdf =
		create("multi-page.pdf");
	
	public static final String html =
		create("test.html");
	
	public static final String doc =
		create("testWORD-Tika.doc");
	
	public static final String docx =
		create("testWORD-Tika.docx");
	
	public static final String archive_zip_rar_7z =
		create("zip-rar-7z.zip");
	
	public static final String truezip_compat_rar =
		create("check-truezip-compatibility/test.rar.zip/test.rar");
	
	public static final String truezip_compat_7z =
		create("check-truezip-compatibility/test.7z.zip/test.7z");
	
	public static final String outlook_test =
		create("test.pst");
	
	public static final String simple_7z =
		create("simple.7z");
	
	public static final String archive_detection =
		create("archive-detection");
	
	public static final String sfx_zip =
		create("sfx-zip.exe");
	
	public static final String sfx_7z =
		create("sfx-7z.exe");
	
	public static final String sfx_rar =
		create("sfx-rar.exe");
	
	public static final String multiple_dirs_7z =
		create("multiple-dirs.7z");
	
	public static final String multiple_dirs_rar =
		create("multiple-dirs.rar");
	
	public static final String index_update_html_in_7z =
		create("index-update-html-in-7z");
	
	public static final String index_update_rename_in_7z =
		create("index-update-rename-in-7z");

}
