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

package net.sourceforge.docfetcher.util.gui.viewer;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * @author Tran Nam Quang
 */
public abstract class ColumnEditSupport<E> {
	
	private ColumnEditSupport() {}

	public static abstract class TextEditSupport<E> extends ColumnEditSupport<E> {
		protected TextEditSupport() {}
		
		protected abstract void setText(@NotNull E element, @NotNull String text);
	}
	
	public static abstract class ComboEditSupport<E, EN extends Enum<EN>> extends ColumnEditSupport<E> {
		private final Class<EN> choiceClass;

		protected ComboEditSupport(@NotNull Class<EN> choiceClass) {
			this.choiceClass = Util.checkNotNull(choiceClass);
		}
		
		@NotNull
		String[] getStringChoices() {
			EN[] choices = choiceClass.getEnumConstants();
			Util.checkThat(choices.length > 0);
			String[] stringChoices = new String[choices.length];
			for (int i = 0; i < choices.length; i++)
				stringChoices[i] = toString(choices[i]);
			return stringChoices;
		}
		
		void setChoice(@NotNull E element, @NotNull String stringChoice) {
			for (EN choice : choiceClass.getEnumConstants()) {
				if (toString(choice).equals(stringChoice)) {
					setChoice(element, choice);
					return;
				}
			}
			throw new IllegalStateException();
		}
		
		protected abstract void setChoice(@NotNull E element, @NotNull EN choice);
		
		@NotNull
		protected abstract String toString(@NotNull EN choice);
	}

}
