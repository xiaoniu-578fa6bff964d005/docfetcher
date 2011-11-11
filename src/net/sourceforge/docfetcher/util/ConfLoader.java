/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.util;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import net.sourceforge.docfetcher.util.annotations.MutableCopy;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
public final class ConfLoader {

	public interface Loadable {
		String name();
		void load(String str);
	}

	public interface Storable extends Loadable {
		String valueToString();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Description {
		String value();
	}
	
	/**
	 * Loads the preferences from the given file, with <tt>containerClass</tt>
	 * as the class containing the preferences enum classes, and returns a list
	 * of entries where the value is missing or does not have the proper
	 * structure.
	 * <p>
	 * If <tt>createIfMissing</tt> is true, the given file is created if it
	 * doesn't exist. Otherwise a {@link FileNotFoundException} is thrown.
	 */
	// TODO doc: containerClass must contain nested enums that implement Loadable or Storable
	@MutableCopy
	public static List<Loadable> load(	File propFile,
										Class<?> containerClass,
										boolean createIfMissing)
			throws IOException, FileNotFoundException {
		if (! propFile.exists()) {
			if (createIfMissing)
				propFile.createNewFile();
			else
				throw new FileNotFoundException();
		}
		InputStream in = null;
		try {
			FileInputStream fin = new FileInputStream(propFile);
			FileLock lock = fin.getChannel().lock(0, Long.MAX_VALUE, true);
			try {
				in = new BufferedInputStream(fin);
				return load(in, containerClass);
			}
			finally {
				lock.release();
			}
		} finally {
			Closeables.closeQuietly(in);
		}
	}
	
	/**
	 * Loads the preferences from the given input stream, with
	 * <tt>containerClass</tt> as the class containing the preferences enum
	 * classes, and returns a list of entries where the value is missing or does
	 * not have the proper structure.
	 * <p>
	 * The caller is responsible for closing the given input stream.
	 */
	// TODO doc: containerClass must contain nested enums that implement Loadable or Storable
	@MutableCopy
	@NotNull
	public static List<Loadable> load(InputStream in, Class<?> containerClass)
			throws IOException {
		InputStreamReader inReader = new InputStreamReader(in, "utf-8");
		Properties props = new Properties();
		props.load(inReader);
		List<Loadable> notLoaded = new ArrayList<Loadable>();
		Set<String> seenEntries = new HashSet<String> ();
		for (Class<? extends Loadable> clazz : ConfLoader.<Loadable>getEnums(containerClass)) {
			for (Loadable entry : clazz.getEnumConstants()) {
				String name = entry.name();
				if (seenEntries.contains(name)) {
					String msg = String.format(
							"Class %s contains duplicate enum entry '%s.%s'.",
							containerClass.getName(), clazz.getSimpleName(), name
					);
					throw new IllegalStateException(msg);
				}
				seenEntries.add(name);
				String prop = props.getProperty(name);
				if (prop == null) {
					notLoaded.add(entry);
					continue;
				}
				try {
					entry.load(prop);
				} catch (Exception e) {
					notLoaded.add(entry);
				}
			}
		}
		return notLoaded;
	}
	
	/**
	 * Returns a list of all enums classes in the given class using reflection.
	 */
	@MutableCopy
	@SuppressWarnings("unchecked")
	private static <T> List<Class<? extends T>> getEnums(Class<?> enclosingClass) {
		List<Class<? extends T>> enums = Lists.newArrayList();
		Class<?>[] nestedClasses = enclosingClass.getDeclaredClasses();
		for (Class<?> nestedClass : nestedClasses) {
			if (! nestedClass.isEnum()) continue;
			Class<?>[] interfaces = nestedClass.getInterfaces();
			assert interfaces.length == 1;
			enums.add((Class<T>) nestedClass);
		}
		return enums;
	}

	/**
	 * Saves the preferences to the given file.
	 */
	// TODO doc: containerClass must contain nested enums that implement Loadable or Storable
	// Description annotation must be present
	public static void save(File confFile,
							Class<?> containerClass,
							String comment) throws IOException {
		boolean useWinSep = Util.IS_WINDOWS || AppUtil.isPortable();
		String lineSep = useWinSep ? "\r\n" : "\n";
		
		if (useWinSep)
			comment = Util.ensureWindowsLineSep(comment.trim());
		else
			comment = Util.ensureLinuxLineSep(comment.trim());
		
		BufferedWriter out = null;
		try {
			FileOutputStream fout = new FileOutputStream(confFile, false);
			FileLock lock = fout.getChannel().lock();
			try {
				out = new BufferedWriter(new OutputStreamWriter(fout, "utf-8"));
				out.write(comment);
				out.write(lineSep);
				out.write("#");
				out.write(lineSep);
				out.write("# ");
				out.write(new Date().toString());
				out.write(lineSep);
				out.write(lineSep);
				
				int i = 0;
				for (Class<? extends Storable> clazz : ConfLoader.<Storable>getEnums(containerClass)) {
					Storable[] entries = clazz.getEnumConstants();
					if (entries.length == 0) continue;
					if (i++ > 0) {
						out.write(lineSep);
						out.write(lineSep);
					}
					
					String description = clazz.getAnnotation(Description.class).value();
					if (useWinSep)
						description = Util.ensureWindowsLineSep(description);
					else
						description = Util.ensureLinuxLineSep(description);
					out.write(description);
					out.write(lineSep);
					
					int j = 0;
					for (Storable entry : entries) {
						if (j++ > 0)
							out.write(lineSep);
						String key = convert(entry.name(), true);
						String value = convert(entry.valueToString(), false);
						out.write(key + " = " + value);
					}
				}
			}
			finally {
				lock.release();
			}
		}
		finally {
			Closeables.closeQuietly(out);
		}
	}

	/**
	 * @see Properties#store(java.io.Writer, String)
	 */
	public static String convert(String input, boolean escapeSpace) {
		StringBuilder out = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch(c) {
			case ' ':
				if (i == 0 || escapeSpace)
					out.append('\\');
				out.append(' ');
				break;
			case '\t': out.append("\\t"); break;
			case '\n': out.append("\\n"); break;
			case '\r': out.append("\\r"); break;
			case '\f': out.append("\\f"); break;
			case '\\': // Fall through
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!': out.append('\\'); out.append(c); break;
			default: out.append(c);
			}
		}
		return out.toString();
	}
	
	// returns success
	// TODO doc: containerClass must contain nested enums that implement Loadable or Storable
	public static boolean loadFromStreamOrFile(	@NotNull Class<?> resourceClass,
	                                           	@NotNull Class<?> classToLoad,
	                                           	@NotNull String confName,
	                                           	@NotNull String confPath) {
		InputStream in = resourceClass.getResourceAsStream(confName);
		try {
			load(in, classToLoad);
		}
		catch (Exception e) {
			try {
				load(new File(confPath), classToLoad, false);
			}
			catch (Exception e1) {
				return false;
			}
		}
		finally {
			Closeables.closeQuietly(in);
		}
		return true;
	}

	private ConfLoader() {}
	
}
