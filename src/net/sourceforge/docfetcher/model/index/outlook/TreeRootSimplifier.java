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

package net.sourceforge.docfetcher.model.index.outlook;

import java.util.Collection;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
abstract class TreeRootSimplifier<E> {
	
	// TODO test
	/**
	 * For the given tree root, returns a new tree root further down the tree so
	 * that empty nodes and branches in between are omitted.
	 */
	@NotNull
	public final E getSimplifiedRoot(@NotNull E rootNode) {
		E current = Util.checkNotNull(rootNode);
		while (true) {
			Collection<E> children = getChildren(current);
			if (children.isEmpty() || hasContent(current))
				break;
			int childrenWithContentCount = 0;
			E childWithContent = null;
			for (E child : children) {
				if (hasDeepContent(child)) {
					childrenWithContentCount++;
					childWithContent = child;
				}
			}
			if (childrenWithContentCount != 1)
				break;
			current = childWithContent;
		}
		return current;
	}
	
	/**
	 * Returns whether the given node has content.
	 */
	protected abstract boolean hasContent(@NotNull E node);
	
	/**
	 * Returns true if the given node has content and/or has at least one direct
	 * or indirect child that has content. Returns false otherwise.
	 */
	protected abstract boolean hasDeepContent(@NotNull E node);
	
	@NotNull
	protected abstract Collection<E> getChildren(@NotNull E node);

}
