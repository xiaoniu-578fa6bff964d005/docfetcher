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

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;
import java.io.IOException;

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.VisibleForPackageGroup;

/**
 * @author Tran Nam Quang
 */
@VisibleForPackageGroup
public enum SolidArchiveFactory {
	
	Rar {
		@NotNull
		public SolidArchiveTree<?> createSolidArchiveTree(	@NotNull SolidArchiveContext context,
															@NotNull File archiveFile)
				throws IOException, ArchiveEncryptedException {
			return new RarTree(
					archiveFile,
					context.getConfig(),
					context.getOriginalPath(),
					context
			);
		}
	},
	
	SevenZip {
		@NotNull
		public SolidArchiveTree<?> createSolidArchiveTree(	@NotNull SolidArchiveContext context,
															@NotNull File archiveFile)
				throws IOException, ArchiveEncryptedException {
			return new SevenZipTree(
					archiveFile,
					context.getConfig(),
					context.getOriginalPath(),
					context
			);
		}
	},
	;
	
	@NotNull
	public abstract SolidArchiveTree<?> createSolidArchiveTree(	@NotNull SolidArchiveContext context,
																@NotNull File archiveFile)
			throws IOException, ArchiveEncryptedException;
	
}