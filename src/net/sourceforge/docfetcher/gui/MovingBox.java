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

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.gui.Col;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Tran Nam Quang
 */
public final class MovingBox {
	
	private static final int bw = 2; // border width
	
	private final Thread thread;
	private final Shell movingBox;
	
	private volatile double progress = 0;
	private Region region;
	
	public MovingBox(	@NotNull final Shell shell,
						@NotNull final Rectangle source,
						@NotNull final Rectangle destination,
						final double speed,
						final long sleepTime) {
		movingBox = new Shell(shell, SWT.NO_TRIM | SWT.APPLICATION_MODAL);
		movingBox.setBackground(Col.DARK_GRAY.get());
		updateRegion(source);
		
		movingBox.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				region.dispose();
			}
		});
		
		// top left corner
		final Vector A1 = new Vector(source.x, source.y);
		final Vector A2 = new Vector(destination.x, destination.y);
		
		// bottom right corner
		final Vector B1 = new Vector(A1.x + source.width, A1.y + source.height);
		final Vector B2 = new Vector(A2.x + destination.width, A2.y + destination.height);
		
		thread = new Thread(MovingBox.class.getSimpleName()) {
			public void run() {
				while (true) {
					try {
						Thread.sleep(sleepTime);
					}
					catch (InterruptedException e) {
						break;
					}
					
					progress = progress * (1 - speed) + speed;
					if (progress > 0.95)
						break;
					
					Vector Am = m(A1, A2);
					Vector Bm = m(B1, B2);
					
					final Rectangle bounds = new Rectangle(
						Am.x, Am.y,
						Bm.x - Am.x, Bm.y - Am.y
					);
					
					Util.runSyncExec(shell, new Runnable() {
						public void run() {
							updateRegion(bounds);
						}
					});
				}
				
				Util.runSyncExec(shell, new Runnable() {
					public void run() {
						movingBox.dispose();
					}
				});
			}
		};
		
		movingBox.open();
	}
	
	public void start() {
		thread.start();
	}
	
	private Vector m(Vector v1, Vector v2) {
		return new Vector(m(v1.x, v2.x), m(v1.y, v2.y));
	}
	
	private int m(int x1, int x2) {
		return (int) Math.round(x1 + ((x2 - x1) * progress));
	}
	
	private void updateRegion(@NotNull Rectangle bounds) {
		movingBox.setBounds(bounds);

		if (region != null)
			region.dispose();
		region = new Region();
		
		region.add(new int[] {
			0, 0,
			bounds.width, 0,
			bounds.width, bounds.height,
			0, bounds.height
		});
		
		region.subtract(new int[] {
			bw, bw,
			bounds.width - bw, bw,
			bounds.width - bw, bounds.height - bw,
			bw, bounds.height - bw
		});
		
		movingBox.setRegion(region);
	}
	
	private static class Vector {
		private final int x;
		private final int y;
		
		public Vector(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

}
