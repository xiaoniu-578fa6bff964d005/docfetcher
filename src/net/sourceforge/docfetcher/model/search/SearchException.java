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

package net.sourceforge.docfetcher.model.search;

/**
 * @author Tran Nam Quang
 */
public final class SearchException extends Exception {
	
	static final long serialVersionUID = 1;
	
	public SearchException(String msg) {
		super(msg);
	}
	
}