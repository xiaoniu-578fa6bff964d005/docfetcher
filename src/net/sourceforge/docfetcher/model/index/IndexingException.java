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

package net.sourceforge.docfetcher.model.index;

import java.io.IOException;

import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.VisibleForPackageGroup;

/**
 * A wrapper for unrecoverable IOExceptions which may occur during indexing and
 * should cause the entire indexing process to terminate as soon as possible.
 * Examples: Corrupted index files or inaccessible temporary directory.
 * <p>
 * This class helps to distinguish between these unrecoverable IOExceptions and
 * other, less severe IOExceptions that result from bad user data and can thus
 * be swallowed locally.
 * 
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
@SuppressWarnings("serial")
public final class IndexingException extends Exception {
	
	private final IOException cause;
	
	public IndexingException(@NotNull IOException e) {
		super(e);
		this.cause = e;
	}
	
	@NotNull
	public IOException getIOException() {
		return cause;
	}

}
