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

package net.sourceforge.docfetcher.model;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public enum DocumentType {
	
	FILE (false),
	OUTLOOK (true),
	;
	
	private final String prefix = name().toLowerCase() + "://";
	private final boolean isEmail;
	
	private DocumentType(boolean isEmail) {
		this.isEmail = isEmail;
	}
	
	@NotNull
	public String createUniqueId(@NotNull Path path) {
		return prefix + path.getPath();
	}
	
	@NotNull
	public static Path extractPath(@NotNull String uid) {
		Util.checkNotNull(uid);
		for (DocumentType type : values())
			if (uid.startsWith(type.prefix))
				return new Path(uid.substring(type.prefix.length()));
		throw new IllegalArgumentException();
	}
	
	public static boolean isEmailType(@NotNull String uid) {
		Util.checkNotNull(uid);
		for (DocumentType type : values())
			if (type.isEmail && uid.startsWith(type.prefix))
				return true;
		return false;
	}
	
}