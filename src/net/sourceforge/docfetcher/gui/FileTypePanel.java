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

package net.sourceforge.docfetcher.gui;

import java.util.Comparator;
import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.ListMap;
import net.sourceforge.docfetcher.base.ListMap.Entry;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.model.parse.Parser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Tran Nam Quang
 */
public final class FileTypePanel {

	public final Event<Void> evtCheckStatesChanged = new Event<Void>();

	private final Table table;

	public FileTypePanel(	@NotNull Composite parent,
							@NotNull ListMap<Parser, Boolean> parserStateMap) {
		table = new Table(parent, SWT.BORDER | SWT.CHECK | SWT.MULTI);

		// Remove selection when viewer loses focus
		table.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				table.deselectAll();
			}
		});

		// Sort by parser name
		parserStateMap.sort(new Comparator<Entry<Parser, Boolean>>() {
			public int compare(	Entry<Parser, Boolean> o1,
								Entry<Parser, Boolean> o2) {
				String label1 = o1.getKey().getTypeLabel();
				String label2 = o2.getKey().getTypeLabel();
				return label1.compareTo(label2);
			}
		});

		// Fill table
		for (Entry<Parser, Boolean> entry : parserStateMap) {
			TableItem item = new TableItem(table, SWT.NONE);
			Parser parser = entry.getKey();
			Boolean checked = entry.getValue();
			item.setText(parser.getTypeLabel());
			item.setData(parser);
			item.setChecked(checked);
		}

		// Handle check state changes
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!Util.contains(e.detail, SWT.CHECK))
					return;
				evtCheckStatesChanged.fire(null);
			}
		});
	}

	@NotNull
	public Control getControl() {
		return table;
	}

	@NotNull
	public ListMap<Parser, Boolean> getParserStateMap() {
		TableItem[] items = table.getItems();
		ListMap<Parser, Boolean> map = new ListMap<Parser, Boolean>(items.length);
		for (TableItem item : items) {
			Parser parser = (Parser) item.getData();
			map.add(parser, item.getChecked());
		}
		return map;
	}

}
