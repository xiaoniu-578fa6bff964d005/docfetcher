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

import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.NotThreadSafe;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.ThreadSafe;
import net.sourceforge.docfetcher.model.FileResource;
import net.sourceforge.docfetcher.model.MailResource;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.model.search.HighlightedString;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.model.search.ResultDocument.PdfPageHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * @author Tran Nam Quang
 */
public final class PreviewPanel extends Composite {
	
	private final TextPreview textPreview;
	@Nullable private EmailPreview emailPreview;
	@Nullable private HtmlPreview htmlPreview;
	private final StackLayout stackLayout;
	
	// These fields should only be accessed from the GUI thread
	@Nullable private ResultDocument lastDoc;
	@Nullable private MailResource lastMailResource;
	@Nullable private FileResource lastHtmlResource;
	
	// Guarded by 'this' lock; used as a thread interrupt signal
	private long requestCount = 0;
	
	public PreviewPanel(@NotNull Composite parent) {
		super(parent, SWT.NONE);
		textPreview = new TextPreview(this);
		setLayout(stackLayout = new StackLayout());
		stackLayout.topControl = textPreview;
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeLastResources();
			}
		});
	}
	
	@ThreadSafe
	public synchronized void setPreview(@NotNull ResultDocument doc) {
		Util.checkNotNull(doc);
		Util.checkThat(Display.getCurrent() != null);
		
		// TODO show 'loading' message where appropriate (-> maybe as a separate composite)
		// TODO display parse errors on a separate bar
		
		if (lastDoc == doc)
			return;
		lastDoc = doc;
		disposeLastResources();
		requestCount++;
		
		if (doc.isEmail()) {
			clearPreviews(true, false, true);
			new EmailThread(doc, requestCount).start();
		}
		else if (doc.isHtmlFile()) {
			clearPreviews(true, true, false);
			new HtmlThread(doc, requestCount).start();
		}
		else if (doc.isPdfFile()) {
			clearPreviews(true, true, true);
			new PdfThread(doc, requestCount).start();
		}
		else {
			clearPreviews(false, true, true);
			new TextThread(doc, requestCount).start();
		}
	}
	
	@NotThreadSafe
	private void clearPreviews(boolean text, boolean email, boolean html) {
		if (text)
			textPreview.clear();
		if (email && emailPreview != null)
			emailPreview.clear();
		if (html && htmlPreview != null)
			htmlPreview.clear();
	}

	@NotThreadSafe
	private void disposeLastResources() {
		if (lastMailResource != null) {
			lastMailResource.dispose();
			lastMailResource = null;
		}
		if (lastHtmlResource != null) {
			lastHtmlResource.dispose();
			lastHtmlResource = null;
		}
	}
	
	// Returns true on success
	@ThreadSafe
	private boolean setTextSafely(	@NotNull final HighlightedString string,
									long requestCount,
									final boolean append) {
		return runSafely(requestCount, new Runnable() {
			public void run() {
				if (append)
					textPreview.appendText(string);
				else
					textPreview.setText(string);
				if (stackLayout.topControl != textPreview) {
					stackLayout.topControl = textPreview;
					layout();
				}
			}
		});
	}
	
	// Returns true on success
	@ThreadSafe
	private synchronized boolean runSafely(	long requestCount,
	                                       	@NotNull Runnable runnable) {
		if (requestCount != this.requestCount)
			return false;
		Util.runSyncExec(this, runnable);
		return true;
	}
	
	private class EmailThread extends PreviewThread {
		public EmailThread(@NotNull ResultDocument doc, long startCount) {
			super(doc, startCount);
		}
		public void run() {
			try {
				// TODO catch OutOfMemoryErrors
				final MailResource mailResource = doc.getMailResource();
				boolean success = runSafely(startCount, new Runnable() {
					public void run() {
						if (emailPreview == null)
							emailPreview = new EmailPreview(PreviewPanel.this);
						emailPreview.setEmail(mailResource);
						if (stackLayout.topControl != emailPreview) {
							stackLayout.topControl = emailPreview;
							layout();
						}
						lastMailResource = mailResource; // Save a reference for disposal
					}
				});
				if (!success)
					mailResource.dispose();
			} catch (ParseException e) {
				// TODO show error message (check thread.isInterrupted and disposal of widgets!)
			}
		}
	}
	
	private class HtmlThread extends PreviewThread {
		public HtmlThread(@NotNull ResultDocument doc, long startCount) {
			super(doc, startCount);
		}
		public void run() {
			try {
				// TODO catch OutOfMemoryErrors
				final FileResource htmlResource = doc.getFileResource();
				boolean success = runSafely(startCount, new Runnable() {
					public void run() {
						if (htmlPreview == null)
							htmlPreview = new HtmlPreview(PreviewPanel.this);
						htmlPreview.setFile(htmlResource.getFile());
						if (stackLayout.topControl != htmlPreview) {
							stackLayout.topControl = htmlPreview;
							layout();
						}
						lastHtmlResource = htmlResource; // Save a reference for disposal
					}
				});
				if (!success)
					htmlResource.dispose();
			} catch (ParseException e) {
				// TODO show error message (check thread.isInterrupted and disposal of widgets!)
			}
		}
	}
	
	private class PdfThread extends PreviewThread {
		public PdfThread(@NotNull ResultDocument doc, long startCount) {
			super(doc, startCount);
		}
		public void run() {
			try {
				doc.readPdfPages(new PdfPageHandler() {
					private boolean isStopped = false;
					public void handlePage(HighlightedString pageText) {
						if (! setTextSafely(pageText, startCount, true))
							isStopped = true;
					}
					public boolean isStopped() {
						return isStopped;
					}
				});
				// TODO catch OutOfMemoryErrors
			} catch (ParseException e) {
				// TODO show error message (check thread.isInterrupted and disposal of widgets!)
			}
		}
	}
	
	private class TextThread extends PreviewThread {
		public TextThread(@NotNull ResultDocument doc, long startCount) {
			super(doc, startCount);
		}
		public void run() {
			try {
				HighlightedString string = doc.getHighlightedText();
				setTextSafely(string, startCount, false);
				// TODO catch OutOfMemoryErrors
			} catch (ParseException e) {
				// TODO show error message (check thread.isInterrupted and disposal of widgets!)
			}
		}
	}
	
	private abstract class PreviewThread extends Thread {
		protected final ResultDocument doc;
		protected final long startCount;

		public PreviewThread(@NotNull ResultDocument doc, long startCount) {
			this.doc = Util.checkNotNull(doc);
			this.startCount = startCount;
		}
	}
	
}
