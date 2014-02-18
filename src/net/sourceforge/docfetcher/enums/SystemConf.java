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

/**
 * This class handles the retrieval of application-wide system constants and
 * allows type safe access to them via nested enums.
 * <p>
 * New constants can be added by adding enum members to the nested enums.
 * CamelCase names are to be preferred to UPPERCASE names since the former make
 * the constants file more readable. Duplicate names (e.g. Const.Bool.Test and
 * Const.Int.Test) are not supported and should be avoided.
 * <p>
 * New enums (not enum <em>members</em>) must implement the
 * <code>Pref.Loadable</code> interface; everything else is handled
 * automatically via reflection.
 * 
 * @author Tran Nam Quang
 */
public final class SystemConf {
	
	// TODO pre-release: remove unused entries
	
	public static enum Bool implements Loadable {
		IsPortable (true),
		IsDevelopmentVersion (false),
		;

		private boolean value;
		Bool(boolean value) {
			this.value = value;
		}
		public boolean get() {
			return value;
		}
		public void load(String str) {
			value = Boolean.parseBoolean(str);
		}
	}

	public static enum Int implements Loadable {
		;

		private int value;
		Int(int value) {
			this.value = value;
		}
		public int get() {
			return value;
		}
		public void load(String str) {
			value = Util.toInt(str, value);
		}
	}

	public static enum Str implements Loadable {
		ProgramName ("Unspecified"),
		ProgramVersion ("Unspecified"),
		BuildDate ("Unspecified"),
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

	private SystemConf () {}

}
