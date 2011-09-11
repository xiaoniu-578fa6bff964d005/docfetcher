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

import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public class IndexingReporter {
	
	public static final IndexingReporter nullReporter = new IndexingReporter();
	
	public void setStartTime(long time) {}
	
	public void setEndTime(long time) {}
	
	public void info(@NotNull IndexingInfo info) {}
	
	public void subInfo(int current, int total) {}
	
	public void fail(@NotNull IndexingError error) {}
	
}
