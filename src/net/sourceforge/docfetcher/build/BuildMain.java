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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sourceforge.docfetcher.Main;
import net.sourceforge.docfetcher.TestFiles;
import net.sourceforge.docfetcher.build.U.LineSep;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.google.common.base.Strings;

/**
 * @author Tran Nam Quang
 */
public final class BuildMain {
	
	public static final String appName = "DocFetcher";
	public static final String version = "1.1";
	
	private static final String packageId = Main.class.getPackage().getName();
	private static final String packagePath = packageId.replace(".", "/");
	private static final String mainPath = "build/tmp/src/" + packagePath;
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm");
	private static final String buildDate = dateFormat.format(new Date());
	
	public static void main(String[] args) throws Exception {
		Util.println("Copying sources to build directory...");
		U.copyDir("src", "build/tmp/src");
		
		// Licenses
		String licensePatterns = U.readPatterns("lib/license_patterns.txt");
		String eplFilename = "epl-v10.html";
		U.copyDir(
			"lib", "build/tmp/licenses", licensePatterns,
			"**/license_patterns.txt");
		U.copyBinaryFile("dist/" + eplFilename, "build/tmp/licenses/docfetcher/"
				+ eplFilename);
		U.zipDir("build/tmp/licenses", "build/tmp/licenses.zip");
		
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
		
		createPortableBuild(recreateJarFile(true, LineSep.WINDOWS));
		createMacOsXBuild(recreateJarFile(false, LineSep.UNIX));
		runTests();
	}
	
	private static File recreateJarFile(boolean isPortable, LineSep lineSep) throws Exception {
		String prefix = isPortable ? "" : "non-";
		Util.println(U.format("Creating %sportable jar file...", prefix));
		
		File systemConfDest = new File(mainPath + "/system.conf");
		systemConfDest.delete();
		
		File programConfDest = new File(mainPath + "/program.conf");
		programConfDest.delete();
		
		File mainJarFile = new File(String.format(
			"build/tmp/%s_%s_%s.jar", packageId, version, buildDate));
		mainJarFile.delete();
		
		U.copyTextFile(
			"dist/system-template.conf",
			systemConfDest.getPath(),
			lineSep,
			"${app_name}", appName,
			"${app_version}", version,
			"${build_date}", buildDate,
			"${is_portable}", String.valueOf(isPortable));
		
		U.copyTextFile(
			"dist/program.conf",
			programConfDest.getPath(),
			lineSep);
		
		Jar jar = new Jar();
		jar.setProject(new Project());
		jar.setDestFile(mainJarFile);
		jar.setBasedir(new File("build/tmp/src"));
		
		Manifest manifest = new Manifest();
		Attribute attr = new Attribute();
		attr.setName("Main-Class");
		attr.setValue(Main.class.getName());
		manifest.addConfiguredAttribute(attr);
		jar.addConfiguredManifest(manifest);
		
		jar.execute();
		return mainJarFile;
	}

	private static void createPortableBuild(File tmpMainJar) throws Exception {
		Util.println("Creating portable build...");
		String releaseDir = U.format("build/%s-%s", appName, version);
		U.copyDir("dist/img", releaseDir + "/img");
		U.copyDir("dist/help", releaseDir + "/help");
		
		String excludedLibs = U.readPatterns("lib/excluded_jar_patterns.txt");
		U.copyFlatten("lib", releaseDir + "/lib", "**/*.jar", excludedLibs);
		U.copyFlatten("lib", releaseDir + "/lib/swt", "**/swt*.jar", null);
		U.copyFlatten("lib", releaseDir + "/lib", "**/*.so, **/*.dll, **/*.dylib", null);
		
		String dstMainJar = U.format(
			"%s/lib/%s", releaseDir, tmpMainJar.getName());
		U.copyBinaryFile(tmpMainJar.getPath(), dstMainJar);
		
		String linuxLauncher = U.format("%s/%s-Linux.sh", releaseDir, appName);
		U.copyTextFile(
			"dist/launcher-linux.sh", linuxLauncher, LineSep.UNIX,
			"${main_class}", Main.class.getName()
		);
		
		// Create DocFetcher.app launcher for Mac OS X
		String macOsXLauncher = U.format("%s/%s.app/Contents/MacOS/%s", releaseDir, appName, appName);
		U.copyTextFile(
			"dist/launcher-macosx-portable.sh",
			macOsXLauncher,
			LineSep.UNIX,
			"${app_name}", appName,
			"${main_class}", Main.class.getName()
		);
		U.copyBinaryFile(
			"dist/DocFetcher.icns",
			U.format("%s/%s.app/Contents/Resources/%s.icns", releaseDir, appName, appName));
		deployInfoPlist(new File(U.format("%s/%s.app/Contents", releaseDir, appName)));
		
		if (Util.IS_LINUX || Util.IS_MAC_OS_X) {
			U.exec("chmod +x %s", Util.getAbsPath(linuxLauncher));
			U.exec("chmod +x %s", Util.getAbsPath(macOsXLauncher));
		}
		else {
			Util.printErr("** Warning: Cannot make the" +
					" launcher shell script executable.");
		}
		
		String exeLauncher = U.format("%s/%s.exe", releaseDir, appName);
		U.copyBinaryFile("dist/DocFetcher.exe", exeLauncher);
		
		String batLauncher = U.format("%s/%s.bat", releaseDir, appName);
		U.copyTextFile(
			"dist/launcher-portable.bat", batLauncher, LineSep.WINDOWS,
			"${main_class}", Main.class.getName());
		
		U.copyTextFile(
			"dist/program.conf", releaseDir + "/conf/program.conf", LineSep.WINDOWS);
		
		U.copyBinaryFile("build/tmp/licenses.zip", releaseDir
				+ "/misc/licenses.zip");
	}
	
	private static void deployInfoPlist(File dstDir) throws Exception {
		U.copyTextFile(
			"dist/Info.plist",
			new File(dstDir, "Info.plist").getPath(),
			LineSep.UNIX,
			"${app_name}", appName,
			"${app_version}", version,
			"${build_date}", buildDate,
			"${package_id}", packageId);
	}
	
	private static void createMacOsXBuild(File tmpMainJar) throws Exception {
		String suffix = Util.IS_MAC_OS_X ? "" : " (without disk image packaging)";
		Util.println(U.format("Creating Mac OS X build%s...", suffix));
		
		String appDir = U.format("build/%s.app", appName);
		String contentsDir = appDir + "/Contents";
		String resourcesDir = contentsDir + "/Resources";
		
		U.copyDir("dist/img", resourcesDir + "/img");
		U.copyDir("dist/help", resourcesDir + "/help");
		U.copyBinaryFile(
			"dist/DocFetcher.icns",
			U.format("%s/%s.icns", resourcesDir, appName));
		deployInfoPlist(new File(contentsDir));
		
		String excludedLibs = U.readPatterns("lib/excluded_jar_patterns.txt");
		U.copyFlatten("lib", resourcesDir + "/lib", "**/*.jar", excludedLibs);
		U.copyFlatten("lib", resourcesDir + "/lib/swt", "**/swt*mac*.jar", null);
		U.copyFlatten("lib", resourcesDir + "/lib", "**/*.dylib", null);
		
		String dstMainJar = U.format(
			"%s/lib/%s", resourcesDir, tmpMainJar.getName());
		U.copyBinaryFile(tmpMainJar.getPath(), dstMainJar);
		
		String launcher = U.format("%s/MacOS/%s", contentsDir, appName);
		U.copyTextFile(
			"dist/launcher-macosx-app.sh", launcher, LineSep.UNIX,
			"${app_name}", appName,
			"${main_class}", Main.class.getName()
		);
		
		if (Util.IS_LINUX || Util.IS_MAC_OS_X) {
			U.exec("chmod +x %s", Util.getAbsPath(launcher));
		}
		else {
			Util.printErr("** Warning: Cannot make the" +
					" launcher shell script executable.");
		}
		
		U.copyBinaryFile("build/tmp/licenses.zip", resourcesDir
				+ "/misc/licenses.zip");
		
		if (Util.IS_MAC_OS_X) {
			String dmgPath = U.format("build/%s-%s.dmg", appName, version);
			U.exec("hdiutil create -srcfolder %s %s", appDir, dmgPath);
			U.exec("hdiutil internet-enable yes " + dmgPath);
		}
	}
	
	private static void runTests() {
		Util.println("Running tests...");
		final List<String> classNames = new ArrayList<String>();
		new FileWalker() {
			protected void handleFile(File file) {
				String name = file.getName();
				if (!name.endsWith(".java"))
					return;
				name = Util.splitFilename(name)[0];
				if (!name.startsWith("Test") && !name.endsWith("Test"))
					return;
				String path = file.getPath();
				int start = "src/".length();
				int end = path.length() - ".java".length();
				path = path.substring(start, end);
				path = path.replace("/", ".").replace("\\", ".");
				if (path.equals(TestFiles.class.getName()))
					return;
				classNames.add(path);
			}
		}.run(new File("src"));
		
		Collections.sort(classNames);
		
		JUnitCore junit = new JUnitCore();
		junit.addListener(new RunListener() {
			public void testFailure(Failure failure) throws Exception {
				Util.printErr(Strings.repeat(" ", 8) + "FAILED");
			}
		});
		
		for (String className : classNames) {
			/*
			 * AppUtil.Const must be cleared before each test, otherwise one
			 * test class could load AppUtil.Const and thereby hide
			 * AppUtil.Const loading failures in subsequent tests.
			 */
			AppUtil.Const.clear();
			try {
				Class<?> clazz = Class.forName(className);
				Util.println(Strings.repeat(" ", 4) + className);
				junit.run(clazz);
			}
			catch (ClassNotFoundException e) {
				Util.printErr(e);
			}
		}
		AppUtil.Const.clear();
	}

}
