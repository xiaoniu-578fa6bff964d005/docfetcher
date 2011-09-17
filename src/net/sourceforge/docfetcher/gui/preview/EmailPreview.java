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

package net.sourceforge.docfetcher.gui.preview;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.SettingsConf.Font;
import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.model.MailResource;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.Col;
import net.sourceforge.docfetcher.util.gui.FormDataFactory;
import net.sourceforge.docfetcher.util.gui.LazyImageCache;
import net.sourceforge.docfetcher.util.gui.TabFolderFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
final class EmailPreview extends Composite {
	
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Util.setCenteredBounds(shell, 400, 300);

		// Load images
		LazyImageCache lazyImageCache = new LazyImageCache(display, SystemConf.Str.ImgDir.get());
		Img.initialize(lazyImageCache);
		
		new EmailPreview(shell);

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
	private final DateFormat dateFormat = new SimpleDateFormat();
	
	@NotNull private HighlightingToolBarWithTextViewer toolBarWithTextViewer;
	@NotNull private Composite headerComp;
	
	@NotNull private StyledText fromField;
	@NotNull private StyledText toField;
	@NotNull private StyledText subjectField;
	@NotNull private StyledText dateField;
	@NotNull private StyledText[] allFields;
	
	public EmailPreview(@NotNull Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FillLayout());

		CTabFolder tabFolder = TabFolderFactory.create(this, false, false, true);
		
		toolBarWithTextViewer = new HighlightingToolBarWithTextViewer(tabFolder);
		tabFolder.setTopRight(toolBarWithTextViewer.getToolBar());

		CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
		tabItem.setText("Email");
		tabItem.setImage(Img.EMAIL.get());

		tabItem.setControl(createEmailTab(tabFolder));
		tabFolder.setSelection(tabItem);
	}
	
	@NotNull
	private Control createEmailTab(@NotNull Composite parent) {
		Composite comp = new Composite(parent, SWT.BORDER);
		comp.setLayout(Util.createGridLayout(1, false, 0, 0));
		
		headerComp = createEmailHeader(comp);
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label sep = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolBarWithTextViewer.createTextViewer(comp).setLayoutData(
			new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return comp;
	}
	
	@NotNull
	private Composite createEmailHeader(@NotNull Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new FormLayout());
		
		Label fromLabel = createHeaderLabel(comp, "From:"); // TODO i18n
		fromField = createHeaderField(comp);
		Label toLabel = createHeaderLabel(comp, "To:"); // TODO i18n
		toField = createHeaderField(comp);
		
		Label subjectLabel = createHeaderLabel(comp, "Subject:"); // TODO i18n
		subjectField = createHeaderField(comp);
		Label dateLabel = createHeaderLabel(comp, "Date:"); // TODO i18n
		dateField = createHeaderField(comp);
		
		allFields = new StyledText[] {
			fromField, toField, subjectField, dateField
		};
		
		int firstColWidth1 = fromLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		int firstColWidth2 = subjectLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		int firstColWidth = Math.max(firstColWidth1, firstColWidth2);
		FormDataFactory fdf = FormDataFactory.getInstance();
		
		fdf.left().top().width(firstColWidth).applyTo(fromLabel);
		fdf.reset().left(fromLabel).top().right(50, -5).applyTo(fromField);
		fdf.reset().left(50, 5).top().applyTo(toLabel);
		fdf.left(toLabel).right().applyTo(toField);
		
		fdf.reset().left().top(fromLabel).bottom().width(firstColWidth).applyTo(subjectLabel);
		fdf.reset().right().top(toField).bottom().applyTo(dateField);
		fdf.right(dateField).applyTo(dateLabel);
		fdf.left(subjectLabel).right(dateLabel).applyTo(subjectField);
		
		return comp;
	}
	
	@NotNull
	private Label createHeaderLabel(@NotNull Composite parent,
									@NotNull String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		label.setFont(Font.SystemBold.get());
		return label;
	}
	
	@NotNull
	private StyledText createHeaderField(@NotNull Composite parent) {
		final StyledText st = new StyledText(parent, SWT.SINGLE | SWT.READ_ONLY);
		st.setBackground(Col.WIDGET_BACKGROUND.get());
		st.setForeground(Col.WIDGET_FOREGROUND.get());
		Util.registerSelectAllKey(st);
		
		/*
		 * Clear selection and reset caret position when the text field looses
		 * focus.
		 */
		st.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				st.setSelection(0);
			}
		});
		
		return st;
	}
	
	public void setEmail(@NotNull MailResource mailResource) {
		// TODO now: Make email addresses clickable -> use HighlightedString to encode links?
		// TODO now: If loading info from the document repository failed, try to load it from the Lucene index
		
		fromField.setText(mailResource.getSender());
		List<String> recipients = mailResource.getRecipients();
		toField.setText(Util.join(", ", recipients));
		if (recipients.size() > 1)
			toField.setToolTipText(Util.join("\n", recipients));
		else
			toField.setToolTipText("");
		subjectField.setText(mailResource.getSubject());
		dateField.setText(dateFormat.format(mailResource.getDate()));
		toolBarWithTextViewer.setText(mailResource.getBody());
		
		// TODO now: set attachments -> maybe do this in separate threads;
		// archives and other unparsable files: show 'open' button
		
		headerComp.layout(); // size of date field may have changed
	}

	public void clear() {
		for (StyledText st : allFields)
			st.setText("");
		toField.setToolTipText("");
		toolBarWithTextViewer.clear();
	}

}
