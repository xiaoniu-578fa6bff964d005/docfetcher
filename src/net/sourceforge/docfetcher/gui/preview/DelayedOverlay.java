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
import net.sourceforge.docfetcher.base.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
final class DelayedOverlay {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Util.setCenteredBounds(shell, 640, 480);

		SashForm sash = new SashForm(shell, SWT.HORIZONTAL);
		final StyledText st1 = new StyledText(sash, SWT.BORDER);
		st1.setText("Click here to show the loading screen.");
		new StyledText(sash, SWT.BORDER);
		
		final DelayedOverlay delayedOverlay = new DelayedOverlay(st1);
		delayedOverlay.setMessage("Loading...");
		
		st1.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				delayedOverlay.show();
			}
		});

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	
	public final class Hider {
		private final long localVersion;
		
		private Hider(long version) {
			this.localVersion = version;
		}

		@ThreadSafe
		public void hide() {
			Util.runSWTSafe(control, new Runnable() {
				public void run() {
					if (localVersion == version)
						doHide();
				}
			});
		}
	}
	
	@Nullable private Shell shell;
	private final Control control;
	private final Color bgColor;
	private volatile long delay = 100;
	private volatile String message = "";
	private volatile long version = 0; // should only be modified in GUI thread
	
	public DelayedOverlay(@NotNull final Control control) {
		Util.checkNotNull(control);
		this.control = control;
		
		bgColor = new Color(control.getDisplay(), 200, 255, 200);
		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (shell != null)
					shell.dispose();
				bgColor.dispose();
			}
		});
		
		ControlAdapter listener = new ControlAdapter() {
			public void controlMoved(ControlEvent e) {
				if (shell != null)
					Util.setCenteredBounds(shell, control);
			}
			public void controlResized(ControlEvent e) {
				if (shell != null)
					Util.setCenteredBounds(shell, control);
			}
		};
		control.addControlListener(listener);
		control.getShell().addControlListener(listener);
	}
	
	public void setDelay(long delay) {
		Util.checkThat(delay >= 0);
		this.delay = delay;
	}
	
	public void setMessage(@NotNull String message) {
		Util.checkNotNull(message);
		this.message = message;
	}
	
	@NotNull
	@ThreadSafe
	public Hider show() {
		final long localVersion = version;
		new Thread(DelayedOverlay.class.getName()) {
			public void run() {
				try {
					Thread.sleep(delay);
				}
				catch (InterruptedException e) {
					return;
				}
				Util.runSyncExec(control, new Runnable() {
					public void run() {
						if (localVersion == version)
							doShow();
					}
				});
			}
		}.start();
		return new Hider(localVersion);
	}
	
	@NotThreadSafe
	private void doShow() {
		if (shell != null)
			return;
		
		shell = new Shell(control.getShell(), SWT.NO_TRIM);
		shell.setLayout(Util.createFillLayout(5));
		shell.setBackground(bgColor);
		shell.setForeground(Col.BLACK.get());
		
		final int vMargin = 5;
		final int hMargin = 20;
		StyledText st = new StyledText(shell, SWT.WRAP | SWT.READ_ONLY);
		st.setText(message);
		st.setCaret(null);
		st.setMargins(hMargin, vMargin, hMargin, vMargin);
		st.setBackground(shell.getBackground());
		st.setForeground(shell.getForeground());
		
		Util.setCenteredBounds(shell, control);
		
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				shell = null;
			}
		});
		
		shell.open();
	}
	
	@NotThreadSafe
	private void doHide() {
		version++;
		if (shell == null)
			return;
		shell.dispose();
	}

}
