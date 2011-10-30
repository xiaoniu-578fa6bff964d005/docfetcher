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

package net.sourceforge.docfetcher.util;

import java.io.File;
import java.io.IOException;

import org.aspectj.lang.annotation.SuppressAjWarnings;

import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public final class SwtJarLoader {
	
	private SwtJarLoader() {
	}
	
	@SuppressAjWarnings
	public static void loadSwtJar() throws IOException {
		String osNamePart = getOsNamePart();
		String osArchPart = getOsArchPart();
		
		/*
		 * Determine the directory containing the SWT jars. For all versions
		 * except the non-portable Mac OS X version, this is simply 'lib/swt'.
		 */
		File swtDir = new File("lib/swt");
		if (osNamePart.contains("mac") && !swtDir.exists()) {
			File altSwtDir = new File("../Resources/lib/swt");
			assert altSwtDir.exists();
			swtDir = altSwtDir;
		}
		
        for (File file : swtDir.listFiles()) {
        	String filename = file.getName();
			if (filename.contains(osNamePart)
					&& filename.endsWith(osArchPart + ".jar")) {
        		ClassPathHack.addFile(file);
        		return;
        	}
        }
        throw new IllegalStateException();
	}
	
	@NotNull
	private static String getOsNamePart() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win"))
			return "win32";
		if (osName.contains("mac"))
			return "macosx";
		if (osName.contains("linux") || osName.contains("nix"))
			return "gtk-linux";
		throw new IllegalStateException();
	}
	
	@NotNull
	private static String getOsArchPart() {
		String arch = System.getProperty("sun.arch.data.model");
		if (arch == null)
			arch = System.getProperty("os.arch").toLowerCase();
		return arch.contains("64") ? "x86_64" : "x86";
	}

}
