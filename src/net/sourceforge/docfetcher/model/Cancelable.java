/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model;

/**
 * @author Tran Nam Quang
 */
public interface Cancelable {
	
	public static final Cancelable nullCancelable = new Cancelable() {
		public boolean isCanceled() {
			return false;
		}
	};
	
	public boolean isCanceled();

}
