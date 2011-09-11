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
import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.base.Util;

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
	
	private static final String appName = "DocFetcher";
	private static final String version = "1.1";
	
	public static void main(String[] args) throws Exception {
		String packageId = Main.class.getPackage().getName();
		String packagePath = packageId.replace(".", "/");
		
		DateFormat format = new SimpleDateFormat("yyyyMMdd-HHmm");
		String buildDate = format.format(new Date());
		
		Util.println("Copying sources to build directory...");
		U.copyDir("src", "build/tmp/src");
		
		String mainPath = "build/tmp/src/" + packagePath;
		U.copyTextFile(
			"dist/system-template.conf",
			mainPath + "/system.conf",
			LineSep.WINDOWS,
			"${app_name}", appName,
			"${app_version}", version,
			"${build_date}", buildDate,
			"${is_portable}", Boolean.TRUE.toString());
		U.copyTextFile(
			"dist/program.conf",
			mainPath + "/program.conf",
			LineSep.WINDOWS);
		
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
		runTests();
	}

	private static void createPortableBuild(File tmpMainJar) throws Exception {
		Util.println("Creating portable build...");
		String releaseDir = U.format("build/%s-%s", appName, version);
		U.copyDir("dist/img", releaseDir + "/img");
		
		String excludedLibs = U.readPatterns("lib/excluded_jar_patterns.txt");
		U.copyFlatten("lib", releaseDir + "/lib", "**/*.jar", excludedLibs);
		U.copyFlatten("lib", releaseDir + "/lib/windows", "**/swt*win32*.jar", null);
		U.copyFlatten("lib", releaseDir + "/lib/linux", "**/swt*linux*.jar", null);
		U.copyFlatten("lib", releaseDir + "/lib", "**/*.so, **/*.dll", null);
		
		String dstMainJar = U.format(
			"%s/lib/%s", releaseDir, tmpMainJar.getName());
		U.copyBinaryFile(tmpMainJar.getPath(), dstMainJar);
		
		String linuxLauncher = U.format("%s/%s.sh", releaseDir, appName);
		U.copyTextFile(
			"dist/launcher.sh", linuxLauncher, LineSep.UNIX,
			"${main_class}", Main.class.getName()
		);
		
		if (Util.IS_LINUX) {
			U.exec("chmod +x %s", Util.getAbsPath(linuxLauncher));
		}
		else {
			Util.printErr("Warning: Cannot make the" +
					"Linux launcher script executable.");
		}
		
		String batLauncher = U.format("%s/%s.bat", releaseDir, appName);
		U.copyTextFile(
			"dist/launcher-portable.bat", batLauncher, LineSep.WINDOWS,
			"${main_class}", Main.class.getName());
		
		U.copyTextFile(
			"dist/program.conf", releaseDir + "/program.conf", LineSep.WINDOWS);
		
		U.copyBinaryFile("build/tmp/licenses.zip", releaseDir
				+ "/misc/licenses.zip");
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
