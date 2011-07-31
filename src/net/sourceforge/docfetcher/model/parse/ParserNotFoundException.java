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

/**
 * @author Tran Nam Quang
 */
public final class ParserNotFoundException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	ParserNotFoundException() {
		super("Could not find suitable parser."); // TODO i18n
	}

}
