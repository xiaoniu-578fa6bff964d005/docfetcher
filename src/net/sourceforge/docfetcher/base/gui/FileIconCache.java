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

package net.sourceforge.docfetcher.base.gui;

import java.io.File;
import java.util.Map;

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Widget;

import com.google.common.collect.Maps;

/**
 * This class serves as a cache for file icons. Its main purpose is to avoid
 * hitting the platform's limit on the number of image handles. See bug #2916975
 * and bug #2829490.
 * 
 * @author Tran Nam Quang
 */
public final class FileIconCache {
	
	private final Map<String, Image> fileIconMap = Maps.newHashMap();
	private final Widget disposeWidget;
	
	// Widget argument: Image cache will be cleared when the given widget is disposed
	public FileIconCache(@NotNull Widget disposeWidget) {
		this.disposeWidget = disposeWidget;
		disposeWidget.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				for (Image image : fileIconMap.values())
					image.dispose();
				fileIconMap.clear();
			}
		});
	}
	
	/**
	 * Returns a file icon for the given file. If none is available, the given
	 * default icon is returned, which may be null. The caller does not need to
	 * dispose of the returned images. This method should not be called from a
	 * non-GUI thread.
	 */
	@Nullable
	public Image getIcon(@NotNull File file, @Nullable Image defaultIcon) {
		return getIcon(file.getName(), defaultIcon);
	}
	
	/**
	 * See {@link #getIcon(File, Image)}.
	 */
	@Nullable
	public Image getIcon(@NotNull String filename, @Nullable Image defaultIcon) {
		String extension = Util.getExtension(filename);
		Image image = fileIconMap.get(extension);
		if (image != null)
			return image;
		Program program = Program.findProgram(extension);
		if (program == null)
			return defaultIcon;
		ImageData imgData = program.getImageData();
		if (imgData == null)
			return defaultIcon;
		image = new Image(disposeWidget.getDisplay(), imgData);
		fileIconMap.put(extension, image);
		return image;
	}

}
