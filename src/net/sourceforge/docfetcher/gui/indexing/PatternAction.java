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

package net.sourceforge.docfetcher.gui.indexing;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
final class PatternAction {
	
	// TODO i18n
	
	enum MatchTarget {
		FILENAME ("Filename"),
		FILEPATH ("Path"),
		;
		public final String displayName;

		private MatchTarget(String displayName) {
			this.displayName = displayName;
		}
	}
	
	enum MatchAction {
		EXCLUDE ("Exclude"),
		DETECT_MIME ("Detect mime type (slower)"),
		;
		public final String displayName;

		private MatchAction(String displayName) {
			this.displayName = displayName;
		}
	}
	
	@NotNull private String regex = "regex";
	@NotNull private MatchTarget target = MatchTarget.FILENAME;
	@NotNull private MatchAction action = MatchAction.EXCLUDE;
	
	public PatternAction() {
	}

	public void setRegex(@NotNull String regex) {
		this.regex = Util.checkNotNull(regex);
	}

	@NotNull
	public String getRegex() {
		return regex;
	}

	public void setTarget(@NotNull MatchTarget target) {
		this.target = Util.checkNotNull(target);
	}

	@NotNull
	public MatchTarget getTarget() {
		return target;
	}

	public void setAction(@NotNull MatchAction action) {
		this.action = Util.checkNotNull(action);
	}

	@NotNull
	public MatchAction getAction() {
		return action;
	}
	
}