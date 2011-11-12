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

package net.sourceforge.docfetcher.enums;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.docfetcher.UtilGlobal;
import net.sourceforge.docfetcher.util.ConfLoader;
import net.sourceforge.docfetcher.util.Util;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;


/**
 * @author Tran Nam Quang
 */
public final class MsgMigrator {

	public static void main(String[] args) throws Exception {
		File oldTransDir = new File("dev/old-translations-from-1.0.3");
		final String enPropName = "Resource.properties";
		
		Properties oldEnProp = UtilGlobal.load(new File(oldTransDir, enPropName));
		List<File> oldPropFiles = Arrays.asList(Util.listFiles(oldTransDir, new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.equals(enPropName);
			}
		}));
		
		final Map<Properties, File > propToFileMap = Maps.newHashMap();
		
		List<Properties> oldProps = Lists.transform(oldPropFiles, new Function<File, Properties>() {
			public Properties apply(File file) {
				try {
					Properties prop = UtilGlobal.load(file);
					propToFileMap.put(prop, file);
					return prop;
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			};
		});
		
		StringBuilder sb0 = new StringBuilder();
		for (Msg msg : Msg.values()) {
			String key = ConfLoader.convert(msg.name(), true);
			String value = ConfLoader.convert(msg.get(), false);
			String comments = msg.getComment();
			
			if (!comments.isEmpty())
				sb0.append("# " + comments + Util.LS);
			sb0.append(key + "=" + value);
			sb0.append(Util.LS);
		}
		File enOutFile = new File(Util.TEMP_DIR, enPropName);
		Files.write(sb0.toString(), enOutFile, Charsets.UTF_8);
		Util.println("File written: " + enOutFile.getPath());
		
		for (Properties oldProp : oldProps) {
			StringBuilder sb = new StringBuilder();
			for (Msg msg : Msg.values()) {
				String key = msg.name();

				String enOldValue = oldEnProp.getProperty(key);
				if (enOldValue == null) // New key?
					continue;
				else if (!enOldValue.equals(msg.get())) // Changed value?
					continue;
				
				String value = oldProp.getProperty(key);
				if (value == null)
					value = enOldValue;
				else if (value.equals("$TODO$"))
					continue;
				
				key = ConfLoader.convert(key, true);
				value = ConfLoader.convert(value, false);
				sb.append(key + "=" + value);
				sb.append(Util.LS);
			}
			
			String filename = propToFileMap.get(oldProp).getName();
			File outFile = new File(Util.TEMP_DIR, filename);
			Files.write(sb.toString(), outFile, Charsets.UTF_8);
			Util.println("File written: " + outFile.getPath());
		}
	}
	
}
