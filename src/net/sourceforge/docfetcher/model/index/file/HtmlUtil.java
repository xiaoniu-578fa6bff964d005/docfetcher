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

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;

/**
 * @author Tran Nam Quang
 */
final class HtmlUtil {
	
	/**
	 * Possible suffixes of HTML folders.
	 */
	private static final String[] HTML_FOLDER_SUFFIXES = new String[] {
		"_archivos",
		"_arquivos",
		"_bestanden",
		"_bylos",
		"-Dateien",
		"_datoteke",
		"_dosyalar",
		"_elemei",
		"_failid",
		"_fails",
		"_fajlovi",
		"_ficheiros",
		"_fichiers",
		"-filer",
		".files",
		"_files",
		"_file",
		"_fitxers",
		"_fitxategiak",
		"_pliki",
		"_soubory",
		"_tiedostot"
	};

	private HtmlUtil() {
	}

	/**
	 * Given an HTML folder, this method extracts the name of the folder without
	 * the HTML suffix and the separator character. For example, if the given
	 * HTML folder has the name "foo_files", then "foo" will be returned.
	 * <p>
	 * Returns null if the given file object is not a folder ending with one of
	 * the known HTML suffixes.
	 */
	public static String getHtmlDirBasename(File dir) {
		return HtmlUtil.getHtmlDirBasename(dir.getName());
	}

	/**
	 * @see getHtmlDirBasename
	 */
	public static String getHtmlDirBasename(String dirname) {
		for (String suffix : HTML_FOLDER_SUFFIXES)
			if (dirname.endsWith(suffix))
				return dirname.substring(0, dirname.length() - suffix.length());
		return null;
	}

}
