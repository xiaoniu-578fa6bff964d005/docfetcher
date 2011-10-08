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

package net.sourceforge.docfetcher.model.index;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sourceforge.docfetcher.model.Path;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
@SuppressWarnings("serial")
public final class PatternAction implements Serializable {
	
	// TODO i18n
	
	public enum MatchTarget {
		FILENAME,
		PATH,
		;
	}
	
	public enum MatchAction {
		EXCLUDE ("Exclude"),
		DETECT_MIME ("Detect mime type (slower)"),
		;
		public final String displayName;

		private MatchAction(String displayName) {
			this.displayName = displayName;
		}
	}
	
	@NotNull private String regex = "regex";
	@Nullable private transient Pattern pattern;
	
	@NotNull private MatchTarget target = MatchTarget.FILENAME;
	@NotNull private MatchAction action = MatchAction.EXCLUDE;
	
	public PatternAction() {
	}
	
	// might throw PatternSyntaxException
	public boolean matches(	@NotNull String filename,
							@NotNull Path path,
							boolean isFile) {
		// TODO post-release-1.1: patterns are currently not applied to regular directories
		if (!isFile)
			return false;
		
		if (pattern == null) // Will be null after serialization
			pattern = Pattern.compile(regex);
		
		switch (target) {
		case FILENAME:
			return pattern.matcher(filename).matches();
		case PATH:
			return pattern.matcher(path.getPath()).matches();
		default:
			throw new IllegalStateException();
		}
	}

	// Allows invalid regexes
	public void setRegex(@NotNull String regex) {
		Util.checkNotNull(regex);
		if (this.regex.equals(regex))
			return;
		this.regex = regex;
		pattern = null;
	}
	
	public boolean validateRegex() {
		if (pattern != null)
			return true;
		try {
			pattern = Pattern.compile(regex);
			return true;
		}
		catch (PatternSyntaxException e) {
			return false;
		}
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