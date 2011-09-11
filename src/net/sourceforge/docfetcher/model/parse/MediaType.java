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

package net.sourceforge.docfetcher.model.parse;

import java.util.Locale;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
final class MediaType {
	
	private MediaType() {
	}
	
	@NotNull
	public static String text(@NotNull String subType) {
		return get("text", subType);
	}
	
	@NotNull
	public static String application(@NotNull String subType) {
		return get("application", subType);
	}
	
	@NotNull
	private static String get(@NotNull String type, @NotNull String subType) {
		Util.checkNotNull(type, subType);
		assert subType.length() > 0;
		assert ! subType.startsWith("/") && ! subType.startsWith("\\");
		return type + "/" + subType.toLowerCase(Locale.ENGLISH);
	}

}
