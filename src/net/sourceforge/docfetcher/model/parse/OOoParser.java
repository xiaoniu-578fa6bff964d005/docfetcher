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

import java.io.File;

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.model.Cancelable;

/**
 * @author Tran Nam Quang
 */
abstract class OOoParser extends FileParser {
	
	// accepts TrueZIP files
	protected ParseResult parse(@NotNull File file,
								@NotNull Cancelable cancelable)
			throws ParseException {
		return null; // TODO
	}

}
