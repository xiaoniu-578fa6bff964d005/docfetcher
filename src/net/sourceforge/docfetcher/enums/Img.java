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

package net.sourceforge.docfetcher.enums;

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.LazyImageCache;
import net.sourceforge.docfetcher.util.gui.LazyImageCache.FilenameProvider;

import org.eclipse.swt.graphics.Image;

/**
 * An application-wide image container. The images are allocated on demand and
 * automatically disposed of after program termination. Usage:
 * <ul>
 * <li>The available images are defined by the constants in this enumeration. A
 * brief name and the filename (with file extension) must be specified.
 * <li>Before accessing the images, {@link #initialize(LazyImageCache)} must be
 * called once. This is usually done after the SWT
 * {@link org.eclipse.swt.widgets.Display Display} has been created.
 * <li>Then, images can be retrieved through the enumeration constants and the
 * {@link #get()} method, e.g. <tt>Img.NAME_OF_IMAGE.get()</tt>.
 * </ul>
 * 
 * @author Tran Nam Quang
 */
public enum Img implements FilenameProvider {
	
	DOCFETCHER_16 ("docfetcher16.png"),
	DOCFETCHER_24 ("docfetcher24.png"),
	DOCFETCHER_32 ("docfetcher32.png"),
	DOCFETCHER_48 ("docfetcher48.png"),
	
	HELP ("help.gif"),
	INDEXING ("indexing.gif"),
	MINIMIZE ("minimize.gif"),
	MAXIMIZE ("maximize.gif"),
	HIDE ("hide.gif"),
	RESTORE ("restore.png"),
	INDEXING_DIALOG ("indexing_dialog.gif"),
	ARROW_LEFT ("arrow_left.gif"),
	ARROW_RIGHT ("arrow_right.gif"),
	ARROW_UP ("arrow_up.gif"),
	ARROW_DOWN ("arrow_down.gif"),
	STOP ("stop.gif"),
	REFRESH ("refresh.gif"),
	WINDOW ("program.gif"),
	FOLDER ("folder.gif"),
	FILE ("file.gif"),
	EMAIL ("email.gif"),
	PACKAGE ("package.gif"),
	CLIPBOARD ("clipboard.gif"),
	ADD ("add.gif"),
	REMOVE ("remove.gif"),
	LIST ("list.gif"), // TODO pre-release: Currently not used
	CHECK ("check.gif"),
	TREE ("tree.gif"),
	BROWSER ("browser.gif"),
	BUILDING_BLOCKS ("building_blocks.gif"),
	HIGHLIGHT ("highlight.gif"),
	;
	
	private static LazyImageCache lazyImageCache;

	public static void initialize(@NotNull LazyImageCache lazyImageCache) {
		Img.lazyImageCache = lazyImageCache;
	}
	
	@NotNull private final String filename;
	
	private Img(@NotNull String filename) {
		this.filename = filename;
	}
	
	/**
	 * Returns the <tt>Image</tt> object corresponding to this enumeration
	 * constant. It is disposed of automatically after program termination. If
	 * the image cannot be loaded, null is returned.
	 */
	@Nullable
	public Image get() {
		return lazyImageCache.getImage(filename);
	}

	@NotNull
	public String getFilename() {
		return filename;
	}

}

