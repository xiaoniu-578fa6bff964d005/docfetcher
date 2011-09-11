/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * An enumeration of possible filesize units (byte, kilobyte, etc.) with the
 * capability of converting values from one unit to another.
 * 
 * @author Tran Nam Quang
 */
public enum FilesizeUnit {

	Byte(0), KB(1), MB(2), GB(3);

	private final int e;

	FilesizeUnit(int e) {
		this.e = e;
	}

	/**
	 * Converts the given filesize in the given unit into a filesize in the unit
	 * of the receiver. When converting from a smaller unit to a larger unit
	 * (e.g. Bytes to Kilobytes), the conversion uses division and rounds the
	 * resulting value. When converting to the same unit or to a smaller unit,
	 * the resulting value is exact.
	 * <p>
	 * If the given size filesize is null, this method returns null.
	 */
	@Nullable
	public Long convert(@Nullable Long size, @Nullable FilesizeUnit unit) {
		if (size == null)
			return null;
		int e2 = unit.e - e;
		if (e2 == 0)
			return size;
		if (e2 > 0) {
			for (int i = 0; i < e2; i++)
				size *= 1024;
			return size;
		}
		int denominator = 1024;
		for (int i = 1; i < -e2; i++)
			denominator *= 1024;
		return Math.round((double) size / denominator);
	}

	/**
	 * Returns all enumerated filesize units as strings.
	 */
	@NotNull
	public static String[] valuesAsStrings() {
		FilesizeUnit[] values = values();
		String[] strings = new String[values.length];
		for (int i = 0; i < values.length; i++)
			strings[i] = values[i].toString();
		return strings;
	}

}
