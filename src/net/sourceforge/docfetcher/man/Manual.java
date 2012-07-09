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
import java.util.Properties;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.build.BuildMain;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.CharsetDetectorHelper;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class Manual {
	
	private enum SubPageProperties {
		Query_Syntax ("query_syntax", "Advanced_Usage"),
		Portable_Repositories ("portable_repos", "Advanced_Usage"),
		Indexing_Options ("indexing_options", "Advanced_Usage"),
		Regular_Expressions ("regular_expressions", "Advanced_Usage"),
		Release_Notification ("release_notification", "Advanced_Usage"),
		
		Memory_Limit ("memory_limit", "Caveats"),
		Preferences ("preferences", "Subpages"),
		;
		private final String propsKey;
		private final String backlink;
		private String pageTitle;
		
		private SubPageProperties(	@NotNull String propsKey,
									@NotNull String backlink) {
			this.propsKey = propsKey;
			this.backlink = backlink;
		}
		
		public static void initPageTitles(@NotNull PageProperties props)
				throws IOException {
			for (SubPageProperties subPageProperties : values()) {
				String key = subPageProperties.propsKey;
				subPageProperties.pageTitle = props.getValue(key);
			}
		}
	}
	
	private static final String packagePath = Manual.class.getPackage().getName().replace('.', '/');
	public static final String manDir = String.format("src/%s", packagePath);
	
	public static void main(String[] args) throws Exception {
		Util.checkThat(args.length > 0);
		String langId = args[0];
		
		File srcDir = new File(manDir + "/" + langId);
		String dstDirName = new Locale(langId).getDisplayName();
		File dstDir = new File("dist/help/" + dstDirName);
		Util.deleteContents(dstDir);
		
		// Recursively walk through man subdirectory and convert markdown to html
		PegDownProcessor processor = new PegDownProcessor(Extensions.TABLES);
		File propsFile = new File(srcDir, "/page.properties");
		PageProperties props = new PageProperties(propsFile);
		SubPageProperties.initPageTitles(props);
		convert(processor, props, srcDir, dstDir, true);
		
		// Deploy files in the man/all directory
		for (File file : Util.listFiles(new File(manDir + "/all"))) {
			String name = file.getName();
			String dstPath = dstDir.getPath() + "/DocFetcher_Manual_files/" + name;
			File dstFile = new File(dstPath);
			Files.createParentDirs(dstFile);
			Files.copy(file, dstFile);
		}
	}
	
	@RecursiveMethod
	private static void convert(@NotNull PegDownProcessor processor,
	                            @NotNull PageProperties props,
								@NotNull File srcDir,
								@NotNull File dstDir,
								boolean isTopLevel) throws IOException {
		for (File mdFile : Util.listFiles(srcDir)) {
			String name = mdFile.getName();
			if (mdFile.isDirectory()) {
				File dstSubDir = new File(dstDir, name);
				dstSubDir.mkdirs();
				convert(processor, props, mdFile, dstSubDir, false);
			}
			else if (mdFile.isFile()) {
				if (mdFile.equals(props.propsFile))
					continue;
				if (!name.endsWith(".markdown")) {
					Files.copy(mdFile, new File(dstDir, name));
					continue;
				}
				Util.println("Converting: " + name);
				String rawText = CharsetDetectorHelper.toString(mdFile);
				String htmlBody = processor.markdownToHtml(rawText);
				String newFilename = Util.splitFilename(name)[0] + ".html";
				File htmlFile = new File(dstDir, newFilename);
				String html = isTopLevel
					? convertMainPage(props, mdFile, htmlBody)
					: convertSubPage(props, mdFile, htmlBody);
				Files.write(html, htmlFile, Charsets.UTF_8);
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
		String template = CharsetDetectorHelper.toString(templateFile);

		Pair[] pairs = new Pair[] {
			props.docfetcherManual, props.author, props.mainFooter };
		
		for (Pair pair : pairs) {
			String key = "${" + pair.key + "}";
			template = UtilGlobal.replace(path, template, key, pair.value);
		}
		
		return UtilGlobal.replace(path, template,
			"${app_name}", BuildMain.appName,
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
		String template = CharsetDetectorHelper.toString(templateFile);

		Pair[] pairs = new Pair[] {
			props.docfetcherManual, props.author, props.backToMainPage };
		for (Pair pair : pairs) {
			String key = "${" + pair.key + "}";
			template = UtilGlobal.replace(path, template, key, pair.value);
		}
		
		String fileBasename = Util.splitFilename(file)[0];
		SubPageProperties subProps = SubPageProperties.valueOf(fileBasename);
		if (subProps == null) {
			String msg = "Could not find subpage properties for file " + file.getPath();
			throw new IllegalStateException(msg);
		}
		
		template = UtilGlobal.replace(path, template, "${subpage_title}", subProps.pageTitle);
		String link = "../DocFetcher_Manual.html#" + subProps.backlink;
		template = UtilGlobal.replace(path, template, "${back_link}", link);
		
		return UtilGlobal.replace(path, template, "${contents}", htmlBody);
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
		private final Properties props;
		
		public final Pair author;
		public final Pair docfetcherManual;
		public final Pair mainFooter;
		public final Pair backToMainPage;

		public PageProperties(@NotNull File propsFile) throws IOException {
			this.propsFile = propsFile;
			props = CharsetDetectorHelper.load(propsFile);
			
			author = getPair("author");
			docfetcherManual = getPair("docfetcher_manual");
			mainFooter = getPair("main_footer");
			backToMainPage = getPair("back_to_main_page");
			
			String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
			mainFooter.value = mainFooter.value.replace("${year}", year);
			mainFooter.value = docfetcherManual.value + " &ndash; " + mainFooter.value;
		}
		
		@NotNull
		private Pair getPair(@NotNull String key) throws IOException {
			return new Pair(key, getValue(key));
		}
		
		@NotNull
		private String getValue(@NotNull String key) throws IOException {
			String value = props.getProperty(key);
			if (value == null) {
				String msg = "Missing property %s in file %s.";
				msg = String.format(msg, key, propsFile.getName());
				throw new IOException(msg);
			}
			return value;
		}
	}

}
