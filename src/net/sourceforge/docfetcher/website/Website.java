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

package net.sourceforge.docfetcher.website;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.util.CharsetDetectorHelper;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author Tran Nam Quang
 */
public final class Website {
	
	private static final String packagePath = Website.class.getPackage().getName().replace('.', '/');
	private static final File websiteDir = new File(String.format("src/%s", packagePath));

	public static void main(String[] args) throws Exception {
		for (File srcDir : Util.listFiles(websiteDir)) {
			if (!srcDir.isDirectory())
				continue;
			if (srcDir.getName().equals("all"))
				continue;
			
			File dstDir = new File("dist/website/" + srcDir.getName());
			dstDir.mkdirs();
			Files.deleteDirectoryContents(dstDir);
			
			PegDownProcessor processor = new PegDownProcessor(Extensions.TABLES);
			File propsFile = new File(srcDir, "/page.properties");
			PageProperties props = new PageProperties(propsFile);
			convertDir(processor, props, srcDir, dstDir);
		}
		
		// Deploy files in the website/all directory
		for (File file : Util.listFiles(new File(websiteDir + "/all"))) {
			File dstFile = new File("dist/website/all", file.getName());
			Files.createParentDirs(dstFile);
			Files.copy(file, dstFile);
		}
	}
	
	private static void convertDir(	@NotNull PegDownProcessor processor,
									@NotNull PageProperties props,
									@NotNull File srcDir,
									@NotNull File dstDir) throws IOException {
		for (File mdFile : Util.listFiles(srcDir)) {
			if (mdFile.equals(props.propsFile))
				continue;
			if (!mdFile.isFile())
				continue;
			String name = mdFile.getName();
			if (!name.endsWith(".markdown"))
				continue;
			
			Util.println("Converting: " + name);
			String rawText = CharsetDetectorHelper.toString(mdFile);
			String htmlBody = processor.markdownToHtml(rawText);
			String newFilename = Util.splitFilename(name)[0] + ".html";
			File htmlFile = new File(dstDir, newFilename);
			String html = convertPage(props, mdFile, htmlBody);
			Files.write(html, htmlFile, Charsets.UTF_8);
		}
	}
	
	private static String convertPage(	@NotNull PageProperties props,
										@NotNull File file,
										@NotNull String htmlBody)
			throws IOException {
		String path = file.getPath();
		File templateFile = new File(websiteDir, "template.html");
		String template = CharsetDetectorHelper.toString(templateFile);

		Pair[] pairs = new Pair[] {
			props.title, props.author, props.keywords, props.description,
			props.overview, props.download, props.screenshots, props.more };
		
		for (Pair pair : pairs) {
			String key = "${" + pair.key + "}";
			template = UtilGlobal.replace(path, template, key, pair.value);
		}
		
		String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
		template = UtilGlobal.replace(path, template, "${year}", year);
		
		String html = UtilGlobal.replace(path, template, "${contents}", htmlBody);
		
		// Insert awards table on main page
		if (file.getName().equals("index.markdown")) {
			File awardsFile = new File(websiteDir, "awards.html");
			String awardsTable = CharsetDetectorHelper.toString(awardsFile);
			html = UtilGlobal.replace(path, html, "${awards_table}", awardsTable);
		}
		return html;
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
		
		public final Pair title;
		public final Pair author;
		public final Pair keywords;
		public final Pair description;
		
		public final Pair overview;
		public final Pair download;
		public final Pair screenshots;
		public final Pair more;

		public PageProperties(@NotNull File propsFile) throws IOException {
			this.propsFile = propsFile;
			props = CharsetDetectorHelper.load(propsFile);
			
			title = getPair("title");
			author = getPair("author");
			keywords = getPair("keywords");
			description = getPair("description");
			
			overview = getPair("overview");
			download = getPair("download");
			screenshots = getPair("screenshots");
			more = getPair("more");
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
