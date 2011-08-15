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

import java.util.Map;

import net.sourceforge.docfetcher.base.Stoppable;

import com.google.common.collect.Maps;

/**
 * @author Tran Nam Quang
 */
class MapValueDiff <K, V, T extends Throwable> extends Stoppable<T> {
	
	private final Map<K, V> left;
	private final Map<K, V> right;
	
	public MapValueDiff(Map<K, V> left, Map<K, V> right) {
		this.left = left;
		this.right = right;
	}
	
	protected final void doRun() {
		Map<K, V> rightCopy = Maps.newHashMap(right);
		for (Map.Entry<K, V> leftEntry : left.entrySet()) {
			if (isStopped()) return;
			K leftKey = leftEntry.getKey();
			V leftValue = leftEntry.getValue();
			V rightValue = rightCopy.remove(leftKey);
			if (rightValue != null)
				handleBoth(leftValue, rightValue);
			else
				handleOnlyLeft(leftValue);
		}
		for (V rightValue : rightCopy.values()) {
			if (isStopped()) return;
			handleOnlyRight(rightValue);
		}
	}
	
	protected void handleOnlyLeft(V leftValue) {}
	
	protected void handleOnlyRight(V rightValue) {}
	
	protected void handleBoth(V leftValue, V rightValue) {}
	
}