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
import java.io.StringReader;
import java.util.Calendar;
import java.util.Locale;
import java.util.Properties;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.build.BuildMain;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.util.collect.ListMap;

import org.pegdown.PegDownProcessor;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class Manual {
	
	private static final String packagePath = Manual.class.getPackage().getName().replace('.', '/');
	private static final String manDir = String.format("src/%s", packagePath);
	
	private static final ListMap<String, String> backLinkMap = ListMap.<String, String> create()
		.add("Portable_Repositories", "Advanced_Usage");

	public static void main(String[] args) throws Exception {
		Util.checkThat(args.length > 0);
		String langId = args[0];
		
		File srcDir = new File(manDir + "/" + langId);
		String dstDirName = new Locale(langId).getDisplayName();
		File dstDir = new File("dist/help/" + dstDirName);
		Files.deleteDirectoryContents(dstDir);
		
		PegDownProcessor processor = new PegDownProcessor();
		File propsFile = new File(srcDir, "/page.properties");
		PageProperties props = new PageProperties(propsFile);
		convert(processor, props, srcDir, dstDir, true);
		
		for (File file : Util.listFiles(new File(manDir + "/all"))) {
			String name = file.getName();
			String dstPath = dstDir.getPath() + "/DocFetcher_Manual_files/" + name;
			Files.copy(file, new File(dstPath));
		}
	}
	
	@RecursiveMethod
	private static void convert(@NotNull PegDownProcessor processor,
	                            @NotNull PageProperties props,
								@NotNull File srcDir,
								@NotNull File dstDir,
								boolean isTopLevel) throws IOException {
		for (File file : Util.listFiles(srcDir)) {
			String name = file.getName();
			if (file.isDirectory()) {
				File dstSubDir = new File(dstDir, name);
				dstSubDir.mkdirs();
				convert(processor, props, file, dstSubDir, false);
			}
			else if (file.isFile()) {
				if (file.equals(props.propsFile))
					continue;
				if (!name.endsWith(".markdown")) {
					Files.copy(file, new File(dstDir, name));
					continue;
				}
				Util.println("Converting: " + name);
				String rawText = Files.toString(file, Charsets.UTF_8);
				String htmlBody = processor.markdownToHtml(rawText);
				String newFilename = Util.splitFilename(name)[0] + ".html";
				File dstFile = new File(dstDir, newFilename);
				String html = isTopLevel
				? convertMainPage(props, file, htmlBody)
					: convertSubPage(props, file, htmlBody);
				Files.write(html, dstFile, Charsets.UTF_8);
			}
		}
	}
	
	@NotNull
	private static String convertMainPage(	@NotNull PageProperties props,
	                                      	@NotNull File file,
											@NotNull String htmlBody)
			throws IOException {
		String path = file.getPath();
		File templateFile = new File(manDir, "template-mainpage.html");
		String template = Files.toString(templateFile, Charsets.UTF_8);

		Pair[] pairs = new Pair[] {
			props.docfetcherManual, props.author, props.mainFooter };
		for (Pair pair : pairs) {
			String key = "${" + pair.key + "}";
			template = UtilGlobal.replace(path, template, key, pair.value);
		}
		
		return UtilGlobal.replace(path, template,
			"${app_name}", BuildMain.appName,
			"${version}", BuildMain.version,
			"${contents}", htmlBody
		);
	}
	
	@NotNull
	private static String convertSubPage(	@NotNull PageProperties props,
	                                     	@NotNull File file,
											@NotNull String htmlBody)
			throws IOException {
		String path = file.getPath();
		File templateFile = new File(manDir, "template-subpage.html");
		String template = Files.toString(templateFile, Charsets.UTF_8);

		Pair[] pairs = new Pair[] {
			props.docfetcherManual, props.author, props.backToMainPage };
		for (Pair pair : pairs) {
			String key = "${" + pair.key + "}";
			template = UtilGlobal.replace(path, template, key, pair.value);
		}
		
		Pair[] optPairs = new Pair[] { props.portableDocRepos };
		for (Pair pair : optPairs) {
			String key = "${" + pair.key + "}";
			try {
				template = UtilGlobal.replace(path, template, key, pair.value);
			}
			catch (Exception e) {
				continue;
			}
		}
		
		String basename = Util.splitFilename(file)[0];
		String linkPart = backLinkMap.getValue(basename);
		String link = "../DocFetcher_Manual.html#" + linkPart;
		template = UtilGlobal.replace(path, template, "${back_link}", link);
		
		return UtilGlobal.replace(path, template,
			"${contents}", htmlBody
		);
	}
	
	private static final class Pair {
		public final String key;
		String value;
		
		public Pair(@NotNull String key, @NotNull String value) {
			this.key = key;
			this.value = value;
		}
	}
	
	private static final class PageProperties {
		private final File propsFile;
		private final Properties props = new Properties();
		
		public final Pair author;
		public final Pair docfetcherManual;
		public final Pair mainFooter;
		public final Pair backToMainPage;
		public final Pair portableDocRepos;

		public PageProperties(@NotNull File propsFile) throws IOException {
			this.propsFile = propsFile;
			String propsFileContents = Files.toString(propsFile, Charsets.UTF_8);
			props.load(new StringReader(propsFileContents));
			
			author = get("author");
			docfetcherManual = get("docfetcher_manual");
			mainFooter = get("main_footer");
			backToMainPage = get("back_to_main_page");
			portableDocRepos = get("portable_doc_repos");
			
			String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
			mainFooter.value = mainFooter.value.replace("${year}", year);
			mainFooter.value = docfetcherManual.value + " &ndash; " + mainFooter.value;
		}
		
		@NotNull
		private Pair get(@NotNull String key) throws IOException {
			String value = props.getProperty(key);
			if (value == null) {
				String msg = "Missing property %s in file %s.";
				msg = String.format(msg, key, propsFile.getName());
				throw new IOException(msg);
			}
			return new Pair(key, value);
		}
	}

}
