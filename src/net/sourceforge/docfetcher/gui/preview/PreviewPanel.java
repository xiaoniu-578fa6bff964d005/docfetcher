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

import net.sourceforge.docfetcher.base.Event;
import net.sourceforge.docfetcher.base.MergingBlockingQueue;
import net.sourceforge.docfetcher.base.Util;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.NotThreadSafe;
import net.sourceforge.docfetcher.base.annotations.Nullable;
import net.sourceforge.docfetcher.base.annotations.ThreadSafe;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.gui.preview.DelayedOverlay.Hider;
import net.sourceforge.docfetcher.model.FileResource;
import net.sourceforge.docfetcher.model.MailResource;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.model.search.HighlightedString;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.model.search.ResultDocument.PdfPageHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

/**
 * @author Tran Nam Quang
 */
public final class PreviewPanel extends Composite {
	
	/*
	 * Mixing custom locking with SWT code has been found to be very dead-lock
	 * prone, so this class relies exclusively on SWT's synchronization
	 * facilities for thread-safety.
	 */
	
	private final TextPreview textPreview;
	@Nullable private EmailPreview emailPreview;
	@Nullable private HtmlPreview htmlPreview;
	private final Composite stackComp;
	private final StackLayout stackLayout;
	private final DelayedOverlay delayedOverlay;
	private final StyledText errorField;
	@Nullable private Color lightRed; // Only allocated when needed
	
	// These fields should only be accessed by the GUI thread
	@Nullable private ResultDocument lastDoc;
	@Nullable private MailResource lastMailResource;
	@Nullable private FileResource lastHtmlResource;
	
	// Thread interrupt signal
	private long requestCount = 0;
	
	public PreviewPanel(@NotNull Composite parent) {
		super(parent, SWT.NONE);
		setLayout(Util.createGridLayout(1, false, 0, 2));
		
		stackComp = new Composite(this, SWT.NONE);
		stackComp.setLayout(stackLayout = new StackLayout());
		stackComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		textPreview = new TextPreview(stackComp);
		stackLayout.topControl = textPreview;
		
		delayedOverlay = new DelayedOverlay(stackComp);
		delayedOverlay.setDelay(500);
		delayedOverlay.setMessage("Loading..."); // TODO i18n
		
		errorField = new StyledText(this, SWT.BORDER | SWT.WRAP | SWT.READ_ONLY);
		errorField.setMargins(5, 5, 5, 5);
		errorField.setCaret(null);
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.exclude = true;
		errorField.setLayoutData(gridData);
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeLastResources();
				if (lightRed != null)
					lightRed.dispose();
			}
		});
		
		textPreview.evtTextToHtmlBt.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				SettingsConf.Bool.PreferHtmlPreview.set(true);
				setPreviewUnchecked(lastDoc);
			}
		});
	}
	
	@ThreadSafe
	public void setPreview(@NotNull ResultDocument doc) {
		Util.checkNotNull(doc);
		Util.assertSwtThread();
		
		if (lastDoc == doc)
			return;
		lastDoc = doc;
		
		setPreviewUnchecked(doc);
	}
	
	@NotThreadSafe
	private void setPreviewUnchecked(@NotNull ResultDocument doc) {
		disposeLastResources();
		requestCount++;
		setError(null, requestCount);

		if (doc.isEmail()) {
			if (emailPreview == null)
				emailPreview = new EmailPreview(stackComp);
			moveToTop(emailPreview);
			clearPreviews(true, false, true);
			new EmailThread(doc, requestCount).start();
		}
		else if (doc.isHtmlFile() && SettingsConf.Bool.PreferHtmlPreview.get()) {
			if (htmlPreview == null) {
				htmlPreview = new HtmlPreview(stackComp);
				htmlPreview.evtHtmlToTextBt.add(new Event.Listener<Void>() {
					public void update(Void eventData) {
						SettingsConf.Bool.PreferHtmlPreview.set(false);
						setPreviewUnchecked(lastDoc);
					}
				});
			}
			moveToTop(htmlPreview);
			clearPreviews(true, true, false);
			new HtmlThread(doc, requestCount).start();
		}
		else if (doc.isPdfFile()) {
			moveToTop(textPreview);
			clearPreviews(true, true, true);
			new PdfThread(doc, requestCount).start();
		}
		else {
			textPreview.setHtmlButtonEnabled(doc.isHtmlFile());
			moveToTop(textPreview);
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
	
	@ThreadSafe
	private void setError(	@Nullable final String message,
							final long requestCount) {
		Util.runSwtSafe(errorField, new Runnable() {
			public void run() {
				if (requestCount != PreviewPanel.this.requestCount)
					return;
				
				boolean show = message != null;
				GridData gridData = (GridData) errorField.getLayoutData();
				boolean wasShown = !gridData.exclude;
				gridData.exclude = !show;
				
				errorField.setText(show ? message : "");
				
				assert (lightRed == null) == !wasShown;
				if (show && !wasShown) {
					lightRed = new Color(getDisplay(), new RGB(0f, 0.5f, 1));
					errorField.setBackground(lightRed);
				}
				else if (!show && wasShown) {
					errorField.setBackground(null);
					lightRed.dispose();
					lightRed = null;
				}
				
				/*
				 * It is important to avoid calling layout() if not needed:
				 * layout() will be extremely slow if there is a lot of text in
				 * the text preview.
				 */
				if (wasShown != show)
					layout();
			}
		});
	}
	
	@NotThreadSafe
	private void moveToTop(@NotNull Control control) {
		Util.checkNotNull(control);
		if (stackLayout.topControl == control)
			return;
		stackLayout.topControl = control;
		stackComp.layout();
	}
	
	// Returns true on success
	@ThreadSafe
	private boolean setTextSafely(	@NotNull final HighlightedString string,
									final long requestCount,
									final boolean append) {
		return runSafely(requestCount, textPreview, new Runnable() {
			public void run() {
				if (append)
					textPreview.appendText(string);
				else
					textPreview.setText(string);
			}
		});
	}
	
	// Returns true on success
	@ThreadSafe
	private boolean runSafely(	final long requestCount,
								@NotNull Widget widget,
								@NotNull final Runnable runnable) {
		final boolean[] success = { true };
		Util.runSwtSafe(widget, new Runnable() {
			public void run() {
				if (requestCount != PreviewPanel.this.requestCount) {
					success[0] = false;
					return;
				}
				runnable.run();
			}
		});
		return success[0];
	}
	
	private class EmailThread extends PreviewThread {
		public EmailThread(@NotNull ResultDocument doc, long startCount) {
			super(doc, startCount);
		}
		protected void doRun(Hider overlayHider) throws ParseException {
			// TODO catch OutOfMemoryErrors
			final MailResource mailResource = doc.getMailResource();
			boolean success = runSafely(startCount, stackComp, new Runnable() {
				public void run() {
					emailPreview.setEmail(mailResource);
					lastMailResource = mailResource; // Save a reference for disposal
				}
			});
			if (!success)
				mailResource.dispose();
		}
	}
	
	private class HtmlThread extends PreviewThread {
		public HtmlThread(@NotNull ResultDocument doc, long startCount) {
			super(doc, startCount);
		}
		protected void doRun(Hider overlayHider) throws ParseException {
			// TODO catch OutOfMemoryErrors
			final FileResource htmlResource = doc.getFileResource();
			boolean success = runSafely(startCount, stackComp, new Runnable() {
				public void run() {
					// TODO Browser may not be available on the system -> will throw an SWTError
					htmlPreview.setFile(htmlResource.getFile());
					lastHtmlResource = htmlResource; // Save a reference for disposal
				}
			});
			if (!success)
				htmlResource.dispose();
		}
	}
	
	private class PdfThread extends PreviewThread {
		private volatile boolean isStopped = false;
		
		public PdfThread(@NotNull ResultDocument doc, long startCount) {
			super(doc, startCount);
		}
		protected void doRun(final Hider overlayHider) throws ParseException {
			class Item {
				@Nullable private final HighlightedString string;
				private boolean isLastItem;
				
				public Item(@Nullable HighlightedString string,
							boolean isLastItem) {
					this.string = string;
					this.isLastItem = isLastItem;
				}
			}
			
			final MergingBlockingQueue<Item> queue = new MergingBlockingQueue<Item>() {
				protected Item merge(Item item1, Item item2) {
					if (item2.string != null)
						item1.string.add(item2.string);
					item1.isLastItem = item1.isLastItem || item2.isLastItem;
					return item1;
				};
			};
			
			final Thread updater = new Thread(PreviewPanel.class.getName() + " (PDF updater)") {
				public void run() {
					while (true) {
						try {
							Item item = queue.take();
							if (item.string != null)
								if (!setTextSafely(item.string, startCount, true))
									isStopped = true;
							overlayHider.hide(); // Hide overlay after first page
							if (item.isLastItem)
								return;
							Thread.sleep(40);
						}
						catch (InterruptedException e) {
							continue;
						}
					}
				}
			};
			updater.start();
			
			doc.readPdfPages(new PdfPageHandler() {
				public void handlePage(HighlightedString pageText) {
					queue.put(new Item(pageText, false));
				}
				public boolean isStopped() {
					return isStopped;
				}
			});
			
			queue.put(new Item(null, true));
			
			// TODO catch OutOfMemoryErrors
		}
	}
	
	private class TextThread extends PreviewThread {
		public TextThread(@NotNull ResultDocument doc, long startCount) {
			super(doc, startCount);
		}
		protected void doRun(Hider overlayHider) throws ParseException {
			HighlightedString string = doc.getHighlightedText();
			setTextSafely(string, startCount, false);
			// TODO catch OutOfMemoryErrors
		}
	}
	
	private abstract class PreviewThread extends Thread {
		protected final ResultDocument doc;
		protected final long startCount;

		public PreviewThread(@NotNull ResultDocument doc, long startCount) {
			super(PreviewThread.class.getName());
			setPriority(MIN_PRIORITY);
			
			this.doc = Util.checkNotNull(doc);
			this.startCount = startCount;
		}
		
		public final void run() {
			Hider hider = delayedOverlay.show();
			try {
				doRun(hider);
			}
			catch (ParseException e) {
				setError("Error: " + e.getMessage(), startCount); // TODO i18n
			}
			finally {
				hider.hide();
			}
		}
		
		protected abstract void doRun(@NotNull Hider overlayHider)
				throws ParseException;
	}
	
}
