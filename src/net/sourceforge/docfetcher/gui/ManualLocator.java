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

package net.sourceforge.docfetcher.gui;

import java.io.File;
import java.util.Locale;

import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
final class ManualLocator {
	
	private ManualLocator() {}
	
	@Nullable
	public static File getManualFile() {
		// TODO test on a platforms and with different locales that this really works
		String helpDirParent;
		if (SystemConf.Bool.IsDevelopmentVersion.get())
			helpDirParent = "dist";
		else if (AppUtil.isPortable() || Util.IS_WINDOWS)
			helpDirParent = Util.USER_DIR_PATH;
		else if (Util.IS_MAC_OS_X)
			helpDirParent = "../Resources";
		else if (Util.IS_LINUX)
			helpDirParent = "/usr/share/doc/docfetcher";
		else
			throw new IllegalStateException();
		
		File manualParentDir = getManualParentDir(helpDirParent);
		if (manualParentDir == null)
			return null;
		
		return new File(manualParentDir, "DocFetcher_Manual.html");
	}
	
	/**
	 * This method takes the path to the parent of the help folder as input and
	 * returns a file representing the parent folder of the manual. The method
	 * tries to return the most specific manual possible and falls back
	 * gradually.
	 * <p>
	 * For example, if the user runs in the locale "de_DE", this method tries to
	 * find the manual in a folder "Germany (Germany)". If there is none, it
	 * looks for a folder with the name "Germany". If that isn't found either,
	 * it will try to return the English version. If not even the latter exists,
	 * null is returned.
	 */
	@Nullable
	private static File getManualParentDir(@NotNull String helpDirParent) {
		File helpDir = new File(helpDirParent, "help"); //$NON-NLS-1$
		
		// The target folder names to search for
		String[] manualDirNames = new String[] {
				Locale.getDefault().getDisplayName(Locale.ENGLISH), // e.g. Germany (Germany)
				new Locale(Locale.getDefault().getLanguage()).getDisplayName(Locale.ENGLISH), // e.g. Germany
				Locale.ENGLISH.getDisplayName(Locale.ENGLISH) // English
		};
		
		// Save matches here; all entries can be null
		File[] matches = new File[3];
		
		// Search for matches
		for (File manualDir : Util.listFiles(helpDir)) {
			if (!manualDir.isDirectory())
				continue;
			for (int i = 0; i < manualDirNames.length; i++) {
				if (manualDir.getName().equals(manualDirNames[i])) {
					matches[i] =  manualDir;
					break;
				}
			}
		}
		
		// Return the most specific match
		for (int i = 0; i < matches.length; i++)
			if (matches[i] != null)
				return matches[i];
		
		return null;
	}

}
