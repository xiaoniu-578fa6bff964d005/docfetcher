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

import java.io.File;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public enum TestFiles {
	
	archive_entry_7z_zip_rar ("zip-rar.7z/"
		+ "test.7z/archive.zip/"
		+ "test.zip/archive.rar/"
		+ "test.rar/test2.txt"),
	archive_entry_zip_zip ("zip.zip/test.zip/test.txt"),
	multi_page_pdf ("multi-page.pdf"),
	html ("test.html"),
	doc ("testWORD-Tika.doc"),
	docx ("testWORD-Tika.docx"),
	archive_zip_rar_7z ("zip-rar-7z.zip"),
	truezip_compat_rar ("check-truezip-compatibility/test.rar.zip/test.rar"),
	truezip_compat_7z ("check-truezip-compatibility/test.7z.zip/test.7z"),
	outlook_test ("test.pst"),
	simple_7z ("simple.7z"),
	archive_detection ("archive-detection"),
	sfx_zip ("sfx-zip.exe"),
	sfx_7z ("sfx-7z.exe"),
	sfx_rar ("sfx-rar.exe"),
	multiple_dirs_7z ("multiple-dirs.7z"),
	multiple_dirs_rar ("multiple-dirs.rar"),
	index_update_html_in_7z ("index-update-html-in-7z"),
	index_update_rename_in_7z ("index-update-rename-in-7z"),
	index_update_html_in_html ("index-update-html-in-html"),
	;
	
	private final String path;
	
	private TestFiles(@NotNull String path) {
		this.path = Util.checkNotNull(path);
	}
	
	@NotNull
	public File get() {
		return new File(getPath());
	}
	
	@NotNull
	public String getPath() {
		return Util.joinPath("dev/test-files", path);
	}
	
	@NotNull
	public File getChild(@NotNull String filename) {
		Util.checkNotNull(filename);
		return new File(getPath(), filename);
	}

}
