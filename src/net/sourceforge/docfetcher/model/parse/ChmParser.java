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
import java.util.Collection;

/**
 * @author Tran Nam Quang
 */
final class ChmParser extends FileParser {

	// TODO websearch: parser
	
	@Override
	protected ParseResult parse(File file,
								ParseContext context) throws ParseException {
		return null;
	}

	protected Collection<String> getExtensions() {
		return null;
	}

	protected Collection<String> getTypes() {
		return null;
	}
	
	public String getTypeLabel() {
		return "CHM";
	}

}
