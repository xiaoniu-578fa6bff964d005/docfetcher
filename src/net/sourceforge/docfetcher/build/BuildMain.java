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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.sourceforge.docfetcher.Main;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.Nullable;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.apache.tools.ant.taskdefs.Zip;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class BuildMain {
	
	private static final String appName = "DocFetcher";
	private static final String version = "1.1";
	
	public static void main(String[] args) throws Exception {
		String packageId = Main.class.getPackage().getName();
		String packagePath = packageId.replace(".", "/");
		
		DateFormat format = new SimpleDateFormat("yyyyMMdd-HHmm");
		String buildDate = format.format(new Date());
		
		Util.println("Copying sources to build directory...");
		copyDir("src", "build/tmp/src");
		
		String mainPath = "build/tmp/src/" + packagePath;
		copyTextFile(
			"dist/system-template.conf",
			mainPath + "/system.conf",
			LineSep.WINDOWS,
			"${app_name}", appName,
			"${app_version}", version,
			"${build_date}", buildDate,
			"${is_portable}", Boolean.TRUE.toString());
		copyTextFile(
			"dist/program.conf",
			mainPath + "/program.conf",
			LineSep.WINDOWS);
		
		String licensePatterns = Util.join(", ",
			"**/*LICENSE*",
			"**/*license*",
			"**/*LICENCE*",
			"**/*licence*",
			"**/about.htm*",
			"**/*_files/*", // HTML folders
			"**/*COPYING*",
			"**/*copying*",
			"**/*LGPL*",
			"**/*lgpl*",
			"**/*CPL*.htm*",
			"**/*cpl*.htm*",
			"**/*EPL*.htm*",
			"**/*epl*.htm"
		);
		String eplFilename = "epl-v10.html";
		copyDir("lib", "build/tmp/licenses", licensePatterns, null);
		copyBinaryFile("dist/" + eplFilename, "build/tmp/licenses/docfetcher/"
				+ eplFilename);
		zipDir("build/tmp/licenses", "build/tmp/licenses.zip");
		
		Util.println("Compiling sources...");
		Javac javac = new Javac();
		javac.setProject(new Project());
		javac.setSrcdir(new Paths().addDirSet("build/tmp/src").get());
		javac.setClasspath(new Paths().addFileSet("lib", "**/*.jar").get());
		javac.setSource("5");
		javac.setDebug(true);
		javac.setOptimize(true);
		javac.setFork(true); // Won't find javac executable without this
		javac.execute();
		
		Util.println("Creating Jar file...");
		Jar jar = new Jar();
		jar.setProject(new Project());
		File mainJarFile = new File(String.format(
			"build/tmp/%s_%s_%s.jar", packageId, version, buildDate));
		jar.setDestFile(mainJarFile);
		jar.setBasedir(new File("build/tmp/src"));
		Manifest manifest = new Manifest();
		Attribute attr = new Attribute();
		attr.setName("Main-Class");
		attr.setValue(Main.class.getName());
		manifest.addConfiguredAttribute(attr);
		jar.addConfiguredManifest(manifest);
		jar.execute();
		
		createPortableBuild(mainJarFile);
	}

	private static void createPortableBuild(File tmpMainJar) throws Exception {
		Util.println("Creating portable build...");
		String releaseDir = format("build/%s-%s", appName, version);
		copyDir("dist/img", releaseDir + "/img");
		
		String excludedLibs = Util.join(", ",
			"**/ant-?.?.?.jar",
			"**/aspectjrt-*.jar",
			"**/*-sources.jar",
			"**/*-src.jar",
			"**/swt*.jar"
		);
		copyFlatten("lib", releaseDir + "/lib", "**/*.jar", excludedLibs);
		copyFlatten("lib", releaseDir + "/lib/windows", "**/swt*win32*.jar", null);
		copyFlatten("lib", releaseDir + "/lib/linux", "**/swt*linux*.jar", null);
		
		String dstMainJar = format(
			"%s/lib/%s", releaseDir, tmpMainJar.getName());
		copyBinaryFile(tmpMainJar.getPath(), dstMainJar);
		
		String linuxLauncher = format("%s/%s.sh", releaseDir, appName);
		copyTextFile(
			"dist/launcher.sh", linuxLauncher, LineSep.UNIX,
			"${main_class}", Main.class.getName()
		);
		
		if (Util.IS_LINUX) {
			exec("chmod +x %s", Util.getAbsPath(linuxLauncher));
		}
		else {
			Util.printErr("Warning: Cannot make the" +
					"Linux launcher script executable.");
		}
		
		String batLauncher = format("%s/%s.bat", releaseDir, appName);
		copyTextFile(
			"dist/launcher-portable.bat", batLauncher, LineSep.WINDOWS,
			"${main_class}", Main.class.getName());
		
		copyTextFile(
			"dist/program.conf", releaseDir + "/program.conf", LineSep.WINDOWS);
		
		copyBinaryFile("build/tmp/licenses.zip", releaseDir
				+ "/misc/licenses.zip");
	}
	
	// --------------- Helper methods below ------------------------------------
	
	private static String format(String format, Object... args) {
		return String.format(format, args);
	}
	
	private static void exec(String format, Object... args) throws Exception {
		Runtime.getRuntime().exec(format(format, args)).waitFor();
	}
	
	private static void copyDir(String srcPath, String dstPath) {
		new Copies().addDir(srcPath).setTodir(dstPath).execute();
	}
	
	private static void copyDir(String srcPath,
								String dstPath,
								@Nullable String include,
								@Nullable String exclude) {
		new Copies().addDir(srcPath, include, exclude)
				.setTodir(dstPath)
				.execute();
	}
	
	private static void copyFlatten(String srcPath,
									String dstPath,
									@Nullable String include,
									@Nullable String exclude) {
		new Copies().addDir(srcPath, include, exclude)
				.setTodir(dstPath)
				.flatten()
				.execute();
	}
	
	private static void copyBinaryFile(String srcPath, String dstPath)
			throws Exception {
		new Copies().addFile(srcPath).setTofile(dstPath).execute();
	}
	
	private enum LineSep {
		UNIX,
		WINDOWS,
	}
	
	private static void copyTextFile(	String srcPath,
										String dstPath,
										LineSep lineSep,
										@Nullable String... replacements)
			throws Exception {
		String contents = read(srcPath);
		switch (lineSep) {
		case UNIX:
			contents = Util.ensureLinuxLineSep(contents);
			break;
		case WINDOWS:
			contents = Util.ensureWindowsLineSep(contents);
			break;
		}
		if (replacements != null) {
			Util.checkThat(replacements.length % 2 == 0);
			for (int i = 0; i < replacements.length; i += 2) {
				String s1 = replacements[i];
				String s2 = replacements[i + 1];
				if (!contents.contains(s1)) {
					String msg = "Text substitution failed: File '%s' does not contain '%s'.";
					throw new IllegalStateException(format(msg, srcPath, s1));
				}
				contents = contents.replace(s1, s2);
			}
		}
		if (!dstPath.endsWith(".sh") && !contents.startsWith("#!") && contents.contains("${"))
			Util.printErr(format("Warning: File '%s' contains "
					+ "suspicious substitution pattern: ${", srcPath));
		write(contents, dstPath);
	}
	
	private static String read(String path) throws Exception {
		return Files.toString(new File(path), Charsets.UTF_8);
	}
	
	private static void write(String contents, String path) throws Exception {
		File dstFile = new File(path);
		Files.createParentDirs(dstFile);
		Files.write(contents, dstFile, Charsets.UTF_8);
	}
	
	private static void zipDir(String srcPath, String dstPath) throws Exception {
		// Tried this with TrueZIP 7.0 and got corrupted zip files
		Zip zip = new Zip();
		zip.setProject(new Project());
		zip.setLevel(9);
		zip.addFileset(new FileSets().setDir(srcPath).get());
		zip.setDestFile(new File(dstPath));
		zip.execute();
	}

}
