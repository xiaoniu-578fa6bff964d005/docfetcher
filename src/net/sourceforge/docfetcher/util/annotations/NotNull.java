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

package net.sourceforge.docfetcher.util.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated type is expected never to be null.
 * 
 * @author Tran Nam Quang
 */
@Target({
	ElementType.FIELD,
	ElementType.LOCAL_VARIABLE,
	ElementType.PARAMETER,
	ElementType.METHOD
})
@Retention(RetentionPolicy.SOURCE)
public @interface NotNull {

}
