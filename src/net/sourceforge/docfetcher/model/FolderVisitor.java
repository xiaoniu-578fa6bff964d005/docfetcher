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

package net.sourceforge.docfetcher.model;

import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.RecursiveMethod;
import net.sourceforge.docfetcher.model.index.Stoppable;

/**
 * @author Tran Nam Quang
 */
public class FolderVisitor<D extends Document<D, F>, F extends Folder<D, F>, T extends Throwable> extends Stoppable<T> {
	
	@Nullable
	private final F root;

	public FolderVisitor(@Nullable F root) {
		this.root = root;
	}

	protected final void doRun() {
		if (root != null)
			visit(root);
	}

	@RecursiveMethod
	private void visit(F folder) {
		if (folder.documents != null) {
			for (D fileDocument : folder.documents.values()) {
				if (isStopped())
					return;
				visitDocument(folder, fileDocument);
			}
		}
		if (folder.subFolders != null) {
			for (F subFolder : folder.subFolders.values()) {
				if (isStopped())
					return;
				visitFolder(folder, subFolder);
				visit(subFolder);
			}
		}
	}

	protected void visitDocument(F parent, D fileDocument) {
	}

	protected void visitFolder(F parent, F folder) {
	}
	
}