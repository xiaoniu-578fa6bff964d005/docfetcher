/*******************************************************************************
 * Copyright (c) 2012 Tran Nam Quang.
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
import java.io.FileWriter;
import java.util.Properties;

import net.sourceforge.docfetcher.util.Util;

import com.google.common.io.Closeables;

/**
 * @author Tran Nam Quang
 */
public final class MsgCreateEnProp {

	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		for (Msg msg : Msg.values()) {
			prop.put(msg.name(), msg.get());
		}
		File outFile = new File(Util.TEMP_DIR, "Resource.properties");
		FileWriter w = new FileWriter(outFile);
		prop.store(w, "");
		Closeables.closeQuietly(w);
		Util.println("File written: " + outFile.getPath());
	}

}
