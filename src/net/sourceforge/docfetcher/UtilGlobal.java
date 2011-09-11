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

package net.sourceforge.docfetcher;

import java.util.Collections;
import java.util.Iterator;

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;

/**
 * @author Tran Nam Quang
 */
public final class UtilGlobal {
	
	private UtilGlobal() {
	}

	@NotNull
	public static <S> Iterable<S> convert(@Nullable final Iterable<? extends S> iterable) {
		if (iterable == null)
			return Collections.emptyList();
		return new Iterable<S> () {
			public Iterator<S> iterator() {
				/*
				 * The inner iterator must be instantiated inside the iterator()
				 * method, because we need a new inner iterator every time
				 * iterator() is called.
				 */
				final Iterator<? extends S> innerIterator = iterable.iterator();
				return new Iterator<S> () {
					public boolean hasNext() {
						return innerIterator.hasNext();
					}
					public S next() {
						return innerIterator.next();
					}
					public void remove() {
						innerIterator.remove();
					}
				};
			}
		};
	}
	
}
