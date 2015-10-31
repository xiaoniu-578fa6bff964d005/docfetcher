/*******************************************************************************
 * Copyright (c) 2015 Nam-Quang Tran.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nam-Quang Tran - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.parse;

public interface PageHandler {
	
	/** Returns whether the parsing process should stop. */
	public boolean handlePage(String pageText);
	
}