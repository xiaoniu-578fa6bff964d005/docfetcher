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

package net.sourceforge.docfetcher.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Nullable;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Collection of build-related utility methods.
 * 
 * @author Tran Nam Quang
 */
final class U {
	
	private U() {}

	static String readPatterns(String filepath) throws Exception {
		File file = new File(filepath);
		List<String> list = new ArrayList<String>();
		for (String line : Files.readLines(file, Charsets.UTF_8)) {
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#"))
				continue;
			list.add(line);
		}
		return Util.join(", ", list);
	}

	static String format(String format, Object... args) {
		return String.format(format, args);
	}
	
	static String removePrefix(String input, String prefix) {
		Util.checkThat(input.startsWith(prefix));
		return input.substring(prefix.length());
	}

	static void exec(String format, Object... args) throws Exception {
		String cmd = format(format, args);
		Runtime.getRuntime().exec(cmd).waitFor();
	}
	
	static void execInDir(File dir, String format, Object... args) throws Exception {
		String cmd = format(format, args);
		Runtime.getRuntime().exec(cmd, null, dir).waitFor();
	}

	static void copyDir(String srcPath, String dstPath) {
		new Copies().addDir(srcPath).setTodir(dstPath).execute();
	}

	static void copyDir(String srcPath,
								String dstPath,
								@Nullable String include,
								@Nullable String exclude) {
		new Copies().addDir(srcPath, include, exclude)
				.setTodir(dstPath)
				.execute();
	}

	static void copyFlatten(String srcPath,
							String dstPath,
							@Nullable String include,
							@Nullable String exclude) {
		new Copies().addDir(srcPath, include, exclude)
				.setTodir(dstPath)
				.flatten()
				.execute();
	}

	static void copyBinaryFile(String srcPath, String dstPath)
			throws Exception {
		new Copies().addFile(srcPath).setTofile(dstPath).execute();
	}

	static void copyTextFile(	String srcPath,
								String dstPath,
								LineSep lineSep,
								@Nullable String... replacements)
			throws Exception {
		String contents = U.read(srcPath);
		switch (lineSep) {
		case UNIX:
			contents = Util.ensureLinuxLineSep(contents);
			break;
		case WINDOWS:
			contents = Util.ensureWindowsLineSep(contents);
			break;
		}
		if (replacements != null)
			contents = UtilGlobal.replace(srcPath, contents, replacements);
		U.write(contents, dstPath);
	}
	
	static String read(String path) throws Exception {
		return Files.toString(new File(path), Charsets.UTF_8);
	}

	static void write(String contents, String path) throws Exception {
		File dstFile = new File(path);
		Files.createParentDirs(dstFile);
		Files.write(contents, dstFile, Charsets.UTF_8);
	}

	// does not preserve executable flag
	static void zipDirContents(String srcPath, String dstPath) throws Exception {
		// Tried this with TrueZIP 7.0 and got corrupted zip files
		Zip zip = new Zip();
		zip.setProject(new Project());
		zip.setLevel(9);
		zip.addFileset(new FileSets().setDir(srcPath).get());
		zip.setDestFile(new File(dstPath));
		zip.execute();
	}
	
	// does not preserve executable flag
	static void zipDir(String srcPath, String dstPath) throws Exception {
		// Tried this with TrueZIP 7.0 and got corrupted zip files
		Zip zip = new Zip();
		zip.setProject(new Project());
		zip.setLevel(9);
		
		File srcFile = new File(srcPath);
		String srcParent = Util.getParentFile(srcFile).getPath();
		String include = srcFile.getName() + "/**";
		FileSet fileset = new FileSets().setDir(srcParent).include(include).get();
		
		zip.addFileset(fileset);
		zip.setDestFile(new File(dstPath));
		zip.execute();
	}
		
	public enum LineSep {
		UNIX,
		WINDOWS,
	}

}
