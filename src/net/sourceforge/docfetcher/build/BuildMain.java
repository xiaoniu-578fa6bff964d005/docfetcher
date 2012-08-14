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
import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.build.U.LineSep;
import net.sourceforge.docfetcher.man.Manual;
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
	private static final String version;

	private static final String packageId = Main.class.getPackage().getName();
	private static final String packagePath = packageId.replace(".", "/");
	private static final String mainPath = "build/tmp/src/" + packagePath;

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm");
	private static final String buildDate = dateFormat.format(new Date());

	static {
		// Read version number from file 'current-version.txt'
		String versionStr = "";
		try {
			versionStr = U.read("current-version.txt");
		}
		catch (Exception e) {
			Util.printErr(e);
			System.exit(0);
		}
		Util.checkThat(versionStr.trim().equals(versionStr));
		Util.checkThat(versionStr.split("\r?\n").length == 1);
		version = versionStr;
	}

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
		U.zipDirContents("build/tmp/licenses", "build/tmp/licenses.zip");

		Util.println("Compiling sources...");
		Javac javac = new Javac();
		javac.setProject(new Project());
		javac.setSrcdir(new Paths().addDirSet("build/tmp/src").get());
		javac.setClasspath(new Paths().addFileSet("lib", "**/*.jar").get());
		javac.setSource("1.6");
		javac.setTarget("1.6");
		javac.setDebug(true);
		javac.setOptimize(true);
		javac.setFork(true); // Won't find javac executable without this
		javac.execute();

		recreateJarFile("", false, LineSep.WINDOWS); // Needed for NSIS script
		File portableJar = recreateJarFile("portable_", true, LineSep.WINDOWS);
		File macOsXJar = recreateJarFile("macosx_", false, LineSep.UNIX);

		rebuildManuals();
		createPortableBuild(portableJar);
		createMacOsXBuild(macOsXJar);
		runTests();
	}

	private static File recreateJarFile(String jarPrefix, boolean isPortable,
			LineSep lineSep) throws Exception {
		String msgPrefix = isPortable ? "" : "non-";
		Util.println(U.format("Creating %sportable jar file...", msgPrefix));

		File systemConfDest = new File(mainPath + "/system-conf.txt");
		systemConfDest.delete();

		File programConfDest = new File(mainPath + "/program-conf.txt");
		programConfDest.delete();

		File mainJarFile = new File(String.format(
			"build/tmp/%s%s_%s_%s.jar", jarPrefix, packageId, version, buildDate));
		mainJarFile.delete();

		U.copyTextFile(
			"dist/system-template-conf.txt",
			systemConfDest.getPath(),
			lineSep,
			"${app_name}", appName,
			"${app_version}", version,
			"${build_date}", buildDate,
			"${is_portable}", String.valueOf(isPortable));

		U.copyTextFile(
			"dist/program-conf.txt",
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

	// Must be run before updating the version numbers in the manual pages
	private static void rebuildManuals() throws Exception {
		for (File dir : Util.listFiles(new File(Manual.manDir))) {
			if (!dir.isDirectory() || dir.getName().equals("all"))
				continue;
			Manual.main(new String[] { dir.getName() });
		}
	}

	private static void createPortableBuild(File tmpMainJar) throws Exception {
		Util.println("Creating portable build...");
		String releaseDir = U.format("build/%s-%s", appName, version);
		U.copyDir("dist/img", releaseDir + "/img");
		U.copyDir("dist/help", releaseDir + "/help");
//		U.copyDir("dist/templates", releaseDir + "/templates");
		U.copyDir("dist/lang", releaseDir + "/lang");
		updateManualVersionNumber(new File(releaseDir, "help"));

		String excludedLibs = U.readPatterns("lib/excluded_jar_patterns.txt");
		U.copyFlatten("lib", releaseDir + "/lib", "**/*.jar", excludedLibs);
		U.copyFlatten("lib", releaseDir + "/lib/swt", "**/swt*.jar", null);
		U.copyFlatten("lib", releaseDir + "/lib", "**/*.so, **/*.dll, **/*.dylib", null);

		String jarName = U.removePrefix(tmpMainJar.getName(), "portable_");
		String dstMainJar = U.format("%s/lib/%s", releaseDir, jarName);
		U.copyBinaryFile(tmpMainJar.getPath(), dstMainJar);

		String linuxLauncher = U.format("%s/%s.sh", releaseDir, appName);
		U.copyTextFile(
			"dist/launchers/launcher-linux.sh", linuxLauncher, LineSep.UNIX,
			"${main_class}", Main.class.getName()
		);

		// Create DocFetcher.app launcher for Mac OS X
		String macOsXLauncher = U.format("%s/%s.app/Contents/MacOS/%s", releaseDir, appName, appName);
		U.copyTextFile(
			"dist/launchers/launcher-macosx-portable.sh",
			macOsXLauncher,
			LineSep.UNIX,
			"${app_name}", appName,
			"${main_class}", Main.class.getName()
		);
		U.copyBinaryFile(
			"dist/DocFetcher.icns",
			U.format("%s/%s.app/Contents/Resources/%s.icns", releaseDir, appName, appName));
		deployInfoPlist(new File(U.format("%s/%s.app/Contents", releaseDir, appName)));

		makeExecutable(
			"Cannot make the portable launcher shell scripts executable.",
			linuxLauncher, macOsXLauncher);

		String exeLauncher = U.format("%s/%s.exe", releaseDir, appName);
		U.copyBinaryFile("dist/launchers/DocFetcher-256.exe", exeLauncher);

		for (int heapSize : new int[] {512, 768, 1024}) {
			String exeName = U.format("%s-%d.exe", appName, heapSize);
			exeLauncher = U.format("%s/misc/%s", releaseDir, exeName);
			U.copyBinaryFile("dist/launchers/" + exeName, exeLauncher);
		}

		String batLauncher = U.format("%s/misc/%s.bat", releaseDir, appName);
		U.copyTextFile(
			"dist/launchers/launcher.bat", batLauncher, LineSep.WINDOWS,
			"${main_class}", Main.class.getName());

		String[] daemonNames = new String[] {
			"docfetcher-daemon-windows.exe", "docfetcher-daemon-linux" };
		for (String daemonName : daemonNames) {
			String dstPath = releaseDir + "/" + daemonName;
			U.copyBinaryFile("dist/daemon/" + daemonName, dstPath);
		}
		makeExecutable("Cannot make the Linux daemon executable.", releaseDir
				+ "/" + daemonNames[1]);

		U.copyTextFile(
			"dist/program-conf.txt", releaseDir + "/conf/program-conf.txt", LineSep.WINDOWS);

		U.copyBinaryFile("build/tmp/licenses.zip", releaseDir
				+ "/misc/licenses.zip");

		/*
		 * Create an empty file 'indexes/.indexes.txt' to let the daemons know
		 * we're the portable version.
		 */
		U.write("", releaseDir + "/indexes/.indexes.txt");

		U.copyTextFile(
			"dist/Readme.txt", releaseDir + "/Readme.txt", LineSep.WINDOWS);

		// Wrap the portable build in a zip archive for deployment
		if (Util.IS_LINUX || Util.IS_MAC_OS_X) {
			String cmd = "zip -r -q %s-%s-portable.zip %s-%s";
			U.execInDir(new File("build"), cmd, appName.toLowerCase(), version, appName, version);
		}
		else {
			String zipPath = U.format("build/%s-%s-portable.zip", appName.toLowerCase(), version);
			U.zipDir(releaseDir, zipPath);
			Util.println("** Warning: Could not preserve executable flags in archive: " + zipPath);
		}
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

	private static void makeExecutable(String errorMessage, String... paths)
			throws Exception {
		if (Util.IS_LINUX || Util.IS_MAC_OS_X) {
			for (String path : paths)
				U.exec("chmod +x %s", Util.getAbsPath(path));
		}
		else {
			Util.printErr("** Warning: " + errorMessage);
		}
	}

	private static void createMacOsXBuild(File tmpMainJar) throws Exception {
		String suffix = Util.IS_MAC_OS_X ? "" : " (without disk image packaging)";
		Util.println(U.format("Creating Mac OS X build%s...", suffix));

		String appDir = U.format("build/%s.app", appName);
		String contentsDir = appDir + "/Contents";
		String resourcesDir = contentsDir + "/Resources";

		U.copyDir("dist/img", resourcesDir + "/img");
		U.copyDir("dist/help", resourcesDir + "/help");
		U.copyDir("dist/lang", resourcesDir + "/lang");
		updateManualVersionNumber(new File(resourcesDir, "help"));
		U.copyBinaryFile(
			"dist/DocFetcher.icns",
			U.format("%s/%s.icns", resourcesDir, appName));
		deployInfoPlist(new File(contentsDir));

		String excludedLibs = U.readPatterns("lib/excluded_jar_patterns.txt");
		U.copyFlatten("lib", resourcesDir + "/lib", "**/*.jar", excludedLibs);
		U.copyFlatten("lib", resourcesDir + "/lib/swt", "**/swt*mac*.jar", null);
		U.copyFlatten("lib", resourcesDir + "/lib", "**/*.dylib", null);

		String jarName = U.removePrefix(tmpMainJar.getName(), "macosx_");
		String dstMainJar = U.format("%s/lib/%s", resourcesDir, jarName);
		U.copyBinaryFile(tmpMainJar.getPath(), dstMainJar);

		String launcher = U.format("%s/MacOS/%s", contentsDir, appName);
		U.copyTextFile(
			"dist/launchers/launcher-macosx-app.sh", launcher, LineSep.UNIX,
			"${app_name}", appName,
			"${main_class}", Main.class.getName()
		);
		makeExecutable(
			"Cannot make the Mac OS X launcher shell script executable.",
			launcher);

		U.copyBinaryFile("build/tmp/licenses.zip", resourcesDir
				+ "/misc/licenses.zip");

		if (Util.IS_MAC_OS_X) {
			String dmgPath = U.format("build/%s-%s.dmg", appName, version);
			U.exec("hdiutil create -srcfolder %s %s", appDir, dmgPath);
		}
	}

	private static void updateManualVersionNumber(File helpDir) throws Exception {
		for (File dir : Util.listFiles(helpDir)) {
			File manFile = new File(dir, "DocFetcher_Manual.html");
			String manPath = manFile.getPath();
			String contents = U.read(manPath);
			contents = UtilGlobal.replace(manPath, contents, "${version}", version);
			U.write(contents, manPath);
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
