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

import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
@SuppressWarnings("serial")
public final class ParseException extends Exception {
	
	public ParseException(@NotNull Throwable cause) {
		super(cause);
	}
	
	public ParseException(@NotNull String message) {
		super(message);
	}
	
	public ParseException(@NotNull String message, @NotNull Throwable cause) {
		super(message, cause);
	}
	
}