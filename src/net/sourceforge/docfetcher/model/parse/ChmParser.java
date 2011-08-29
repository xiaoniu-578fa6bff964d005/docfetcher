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

import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.index.IndexingReporter;

/**
 * @author Tran Nam Quang
 */
final class ChmParser extends FileParser {

	protected ParseResult parse(File file,
								IndexingReporter reporter,
								Cancelable cancelable) throws ParseException {
		return null; // TODO
	}

	protected Collection<String> getExtensions() {
		return null; // TODO
	}

	protected Collection<String> getTypes() {
		return null; // TODO
	}
	
	public String getTypeLabel() {
		return "CHM"; // TODO
	}

}
