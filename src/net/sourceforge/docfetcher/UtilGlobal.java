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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Iterator;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.annotations.RecursiveMethod;

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
	
	@NotNull
	public static String[] splitAtExisting(@NotNull String path)
			throws FileNotFoundException {
		return splitAtExisting(path, "");
	}

	@NotNull
	@RecursiveMethod
	private static String[] splitAtExisting(@NotNull String left,
											@NotNull String right)
			throws FileNotFoundException {
		if (new File(left).isFile())
			return new String[] { left, right };
		
		// Reached left end of a relative path
		if (!left.contains("/") && !left.contains("\\"))
			throw new FileNotFoundException();
		
		String[] leftParts = Util.splitPathLast(left);
		
		// Reached Unix root
		if (leftParts[0].length() == 0)
			throw new FileNotFoundException();
		
		// Reached Windows root
		if (leftParts[0].matches("[a-zA-Z]:"))
			throw new FileNotFoundException();
		
		// Move by one path part to the left and recurse
		if (right.length() == 0)
			return splitAtExisting(leftParts[0], leftParts[1]);
		String newRight = Util.joinPath(leftParts[1], right);
		return splitAtExisting(leftParts[0], newRight);
	}
	
}
