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

package net.sourceforge.docfetcher.base.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element is only marked public for usage by
 * packages related to the current package, e.g. superpackages and subpackages.
 * 
 * @author Tran Nam Quang
 */
@Target({
	ElementType.TYPE,
	ElementType.FIELD,
	ElementType.METHOD,
	ElementType.CONSTRUCTOR
})
@Retention(RetentionPolicy.SOURCE)
public @interface VisibleForPackageGroup {

}
