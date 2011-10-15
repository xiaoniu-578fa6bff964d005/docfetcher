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

package net.sourceforge.docfetcher.util.gui.dialog;

import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
public abstract class InputLoop <T> {
	
	@Nullable
	public final T run(@Nullable String startValue) {
		String lastValue = startValue;
		while (true) {
			String newValue = getNewValue(lastValue);
			if (newValue == null)
				return null;
			String denyMsg = getDenyMessage(newValue);
			if (denyMsg != null) {
				AppUtil.showError(denyMsg, true, true);
				lastValue = newValue;
				continue;
			}
			return onAccept(newValue);
		}
	}
	
	@Nullable
	protected abstract String getNewValue(@NotNull String lastValue);
	
	@Nullable
	protected abstract String getDenyMessage(@NotNull String newValue);
	
	@Nullable
	protected abstract T onAccept(@NotNull String newValue);
	
}
