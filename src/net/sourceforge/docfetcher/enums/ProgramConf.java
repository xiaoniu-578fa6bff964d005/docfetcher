/*******************************************************************************
 * Copyright (c) 2010 Tran Nam Quang.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.ConfLoader.Loadable;
import net.sourceforge.docfetcher.util.annotations.Immutable;

import org.aspectj.lang.annotation.SuppressAjWarnings;

/**
 * This class handles the retrieval of application-wide, <em>unmodifiable</em>
 * preferences and allows type safe access to them via nested enums. The default
 * values of the preferences are hardcoded so as to avoid program corruption
 * caused by manipulation of the preferences file by users.
 * <p>
 * New preferences entries can be added by adding enum members to the nested
 * enums. CamelCase names are to be preferred to UPPERCASE names since the
 * former make the preferences file more readable. Duplicate names (e.g.
 * Conf.Bool.Test and Conf.Int.Test) are not supported and should be avoided.
 * <p>
 * New enums (not enum <em>members</em>) must implement the
 * <code>Pref.Loadable</code> interface; everything else is handled
 * automatically via reflection.
 * 
 * @author Tran Nam Quang
 */
public final class ProgramConf {
	
	// TODO pre-release: remove unused entries

	public static enum Bool implements Loadable {
		FixWindowSizes (false),
		CurvyTabs (false),
		ColoredTabs (false),
		;

		private boolean value;
		Bool(boolean value) {
			this.value = value;
		}
		@SuppressAjWarnings
		public boolean get() {
			return value;
		}
		public void load(String str) {
			value = Boolean.parseBoolean(str);
		}
	}

	public static enum Int implements Loadable {
		SearchHistorySize (20, 1),
		MaxLinesInProgressPanel (1000, 2),
		SearchBoxMaxWidth (200, 0),
		MaxResultsTotal (10000, 1),
		WebInterfacePageSize (50, 1),
		;

		private int value;
		private final int min;
		private final int max;
		
		Int(int value, int min, int max) {
			this.value = value;
			this.min = min;
			this.max = max;
		}
		Int(int value, int min) {
			this(value, min, Integer.MAX_VALUE);
		}
		
		public int get() {
			return value;
		}
		public void load(String str) {
			value = Util.clamp(Util.toInt(str, value), min, max);
		}
	}

	public static enum Str implements Loadable {
		AppName ("DocFetcher"),
		CustomTempDir (""),
		;

		private String value;
		Str(String value) {
			this.value = value;
		}
		public String get() {
			return value;
		}
		public void load(String str) {
			value = str;
		}
		public File getFile() {
			return new File(value);
		}
	}

	public static enum IntArray implements Loadable {
		;

		private int[] value;
		IntArray(int... value) {
			this.value = value;
		}
		public int[] get() {
			return value;
		}
		public void load(String str) {
			value = Util.toIntArray(str, value);
		}
	}

	public static enum StrList implements Loadable {
		HtmlExtensions ("html", "htm", "xhtml", "shtml", "shtm", "php", "asp", "jsp")
		;

		private List<String> value;
		StrList(String... value) {
			this.value = Arrays.asList(value);
		}
		@Immutable
		public List<String> get() {
			return Collections.unmodifiableList(value);
		}
		public void load(String str) {
			value = Util.decodeStrings(';', str);
		}
	}

	private ProgramConf () {}

}
