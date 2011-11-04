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

package net.sourceforge.docfetcher.man;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.build.BuildMain;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

import org.pegdown.PegDownProcessor;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class Manual {
	
	private static final String mainFooter
		= "DocFetcher Manual - Copyright (c) 2007-${year} DocFetcher Development Team";
	
	private static final String packagePath = Manual.class.getPackage().getName().replace('.', '/');
	private static final String manDir = String.format("src/%s", packagePath);

	public static void main(String[] args) throws Exception {
		Util.checkThat(args.length > 0);
		String langId = args[0];
		
		File srcDir = new File(manDir + "/" + langId);
		String dstDirName = new Locale(langId).getDisplayName();
		File dstDir = new File("dist/help/" + dstDirName);
		Files.deleteDirectoryContents(dstDir);
		
		PegDownProcessor processor = new PegDownProcessor();
		convert(processor, srcDir, dstDir, true);
		
		for (File file : Util.listFiles(new File(manDir + "/all"))) {
			String name = file.getName();
			String dstPath = dstDir.getPath() + "/DocFetcher_Manual_files/" + name;
			Files.copy(file, new File(dstPath));
		}
	}
	
	@RecursiveMethod
	private static void convert(@NotNull PegDownProcessor processor,
								@NotNull File srcDir,
								@NotNull File dstDir,
								boolean isTopLevel) throws IOException {
		for (File file : Util.listFiles(srcDir)) {
			String name = file.getName();
			if (file.isDirectory()) {
				File dstSubDir = new File(dstDir, name);
				dstSubDir.mkdirs();
				convert(processor, file, dstSubDir, false);
			}
			else if (file.isFile()) {
				if (name.endsWith(".markdown")) {
					Util.println("Converting: " + name);
					String rawText = Files.toString(file, Charsets.UTF_8);
					String htmlBody = processor.markdownToHtml(rawText);
					String newFilename = Util.splitFilename(name)[0] + ".html";
					File dstFile = new File(dstDir, newFilename);
					String html = isTopLevel
						? convertMainPage(file.getPath(), htmlBody)
						: convertSubPage(file.getPath(), htmlBody);
					Files.write(html, dstFile, Charsets.UTF_8);
				}
				else {
					Files.copy(file, new File(dstDir, name));
				}
			}
		}
	}
	
	@NotNull
	private static String convertMainPage(	@NotNull String path,
											@NotNull String htmlBody)
			throws IOException {
		File templateFile = new File(manDir, "template-mainpage.html");
		String template = Files.toString(templateFile, Charsets.UTF_8);
		
		String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
		String footer = mainFooter.replace("${year}", year);
		
		return UtilGlobal.replace(path, template,
			"${main_title}", "DocFetcher Manual",
			"${author}", "DocFetcher Development Team",
			"${app_name}", BuildMain.appName,
			"${version}", BuildMain.version,
			"${contents}", htmlBody,
			"${main_footer}", footer
		);
	}
	
	@NotNull
	private static String convertSubPage(	@NotNull String path,
											@NotNull String htmlBody)
			throws IOException {
		File templateFile = new File(manDir, "template-subpage.html");
		String template = Files.toString(templateFile, Charsets.UTF_8);
		// TODO now
		return UtilGlobal.replace(path, template,
//			"${main_title}", "DocFetcher Manual",
//			"${author}", "DocFetcher Development Team",
//			"${app_name}", BuildMain.appName,
//			"${version}", BuildMain.version,
			"${contents}", htmlBody
		);
	}

}
