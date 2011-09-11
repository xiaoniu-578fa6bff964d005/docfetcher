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

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.util.Stoppable;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
abstract class HtmlFileWalker extends Stoppable <Exception> {
	
	private final File rootDir;
	private final IndexingConfig config;
	
	public HtmlFileWalker(	@NotNull File rootDir,
							@NotNull IndexingConfig config) {
		Util.checkNotNull(rootDir, config);
		this.rootDir = rootDir;
		this.config = config;
	}

	protected final void doRun() {
		run(rootDir);
	}
	
	private void run(@NotNull File parentDir) {
		new HtmlFileLister <Exception> (parentDir, config, null) {
			protected void handleFile(File file) {
				if (HtmlFileWalker.this.isStopped())
					stop(); // Stop HTMML file lister
				else
					HtmlFileWalker.this.handleFile(file);
			}
			protected void handleHtmlPair(	File htmlFile,
											File htmlDir) {
				if (HtmlFileWalker.this.isStopped())
					stop(); // Stop HTMML file lister
				else
					HtmlFileWalker.this.handleHtmlPair(htmlFile, htmlDir);
			}
			protected void handleDir(File dir) {
				if (HtmlFileWalker.this.isStopped()) {
					stop(); // Stop HTMML file lister
					return;
				}
				HtmlFileWalker.this.handleDir(dir);
				HtmlFileWalker.this.run(dir);
			}
			protected boolean skip(File fileOrDir) {
				return HtmlFileWalker.this.skip(fileOrDir);
			}
		}.runSilently();
	}
	
	protected abstract void handleFile(@NotNull File file);
	
	protected abstract void handleHtmlPair(	@NotNull File htmlFile,
											@NotNull File htmlDir);
	
	protected abstract void handleDir(@NotNull File dir);
	
	protected abstract boolean skip(@NotNull File fileOrDir);

}
