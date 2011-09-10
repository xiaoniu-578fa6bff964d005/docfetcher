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

package net.sourceforge.docfetcher.base.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.docfetcher.base.AppUtil;
import net.sourceforge.docfetcher.base.LazyList;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

/**
 * @author Tran Nam Quang
 */
public final class LazyImageCache {
	
	public interface FilenameProvider {
		@NotNull String getFilename();
	}
	
	@Nullable private Map<String, Image> imageMap = Maps.newHashMap();
	private final Display display;
	private final String imageDir;
	
	public LazyImageCache(@NotNull Display display, @NotNull String imageDir) {
		Util.checkNotNull(display, imageDir);
		this.display = display;
		this.imageDir = imageDir;
		display.disposeExec(new Runnable() {
			public void run() {
				for (Image image : imageMap.values())
					image.dispose();
				imageMap.clear();
				imageMap = null;
			}
		});
	}
	
	// Returns image from internal map or loads it from disk if not found in map
	// The returned image is disposed of automatically when the display is disposed
	@Nullable
	public Image getImage(@Nullable String filename) {
		if (imageMap == null) // map is null after display is disposed
			return null;
		Image image = imageMap.get(filename);
		if (image == null) {
			try {
				image = new Image(display, new File(imageDir, filename).getPath());
				imageMap.put(filename, image);
			} catch (Exception e) {
				return null;
			}
		}
		return image;
	}
	
	public <T extends Enum<T> & FilenameProvider> void reportMissingFiles(	@NotNull Shell shell,
	                                                                      	@NotNull Class<T> clazz,
																			@NotNull String errorMessage) {
		/*
		 * Note: We don't really need the shell argument here, but it will make
		 * sure this method isn't called before any shells are created. This is
		 * a requirement, as the showError method below depends on a shell to be
		 * available.
		 */
		LazyList<File> missingFiles = new LazyList<File>();
		for (FilenameProvider provider : clazz.getEnumConstants()) {
			File file = new File(imageDir, provider.getFilename());
			if (!file.isFile())
				missingFiles.add(file);
		}
		if (missingFiles.isEmpty())
			return;
		
		// Display the first missing files
		List<String> firstItems = new ArrayList<String> ();
		int counter = 0;
		for (File file : missingFiles) {
			if (counter >= 5) {
				firstItems.add("...");
				break;
			}
			firstItems.add(Util.getSystemAbsPath(file));
			counter++;
		}
		AppUtil.showError(
				errorMessage + "\n" + Joiner.on("\n").join(firstItems),
				true, false
		);
	}

}
