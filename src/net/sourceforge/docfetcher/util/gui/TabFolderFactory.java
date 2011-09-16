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

package net.sourceforge.docfetcher.util.gui;

import net.sourceforge.docfetcher.util.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

public class TabFolderFactory {
	
	private TabFolderFactory() {}
	
	public static CTabFolder create(Composite parent,
									boolean close,
									boolean curvyTabs,
									boolean coloredTabs) {
		int style = SWT.BORDER;
		if (close)
			style |= SWT.CLOSE;
		CTabFolder tabFolder;
		if (curvyTabs) {
			tabFolder = new CTabFolder(parent, style);
			tabFolder.setSimple(false);
			if (coloredTabs) {
				// TODO now: listen to SWT.Settings event -> put this in the changelog
				Color colBack = Col.TITLE_BACKGROUND.get();
				Color colFront = Col.TITLE_FOREGROUND.get();
				if (! colFront.equals(colBack)) {
					tabFolder.setSelectionBackground(colBack);
					tabFolder.setSelectionForeground(colFront);
				}
			}
		} else {
			tabFolder = new CTabFolder(parent, style);
			tabFolder.setRenderer(new CustomRenderer(tabFolder, coloredTabs));
		}
		increaseTabHeight(tabFolder);
		return tabFolder;
	}
	
	private static void increaseTabHeight(CTabFolder tabFolder) {
		GC gc = new GC(tabFolder.getDisplay());
		int fontHeight = gc.getFontMetrics().getHeight();
		tabFolder.setTabHeight(fontHeight * 2);
		gc.dispose();
	}
	
	private static class CustomRenderer extends CTabFolderRenderer {
		private CTabFolder tabFolder;
		private boolean coloredTabs;
		public CustomRenderer(CTabFolder tabFolder, boolean coloredTabs) {
			super(tabFolder);
			this.tabFolder = tabFolder;
			this.coloredTabs = coloredTabs;
		}
		protected void draw(int part, int state, Rectangle bounds, GC gc) {
			// Tab states
			boolean isHot = Util.contains(state, SWT.HOT);
			boolean isSelected = Util.contains(state, SWT.SELECTED);
			if (Col.TITLE_FOREGROUND.get().equals(Col.TITLE_BACKGROUND.get()))
				coloredTabs = false; // colored tabs sometimes make the text indiscernible
			
			// Color definitions
			Col borderCol = coloredTabs ?
					Col.TITLE_BACKGROUND : Col.WIDGET_NORMAL_SHADOW;
			Col backCol = null;
			if (isHot || isSelected)
				backCol = coloredTabs ?
						Col.TITLE_BACKGROUND : Col.WIDGET_HIGHLIGHT_SHADOW;
			else
				backCol = Col.WIDGET_BACKGROUND;
			Col shadowCol = Col.WIDGET_DARK_SHADOW;
			Col textCol = coloredTabs && (isSelected || isHot) ?
					Col.TITLE_FOREGROUND : Col.WIDGET_FOREGROUND;
			
			// Draw separating line between tabs and body
			if (Util.contains(part, CTabFolderRenderer.PART_HEADER)) {
				int tabHeight = tabFolder.getTabHeight();
				gc.setForeground(borderCol.get());
				gc.drawLine(
						bounds.x,
						bounds.y + tabHeight,
						bounds.x + bounds.width,
						bounds.y + tabHeight
				);
				return;
			}
			
			if(part < 0) {
				super.draw(part, state, bounds, gc);
				return;
			}
			
			CTabItem item = tabFolder.getItem(part);
			int margin = 2;
			int vShift = 2;
			int hotShift = isHot ? -1 : 0;
			gc.setAntialias(SWT.ON);
			
			Rectangle rect = new Rectangle(
					bounds.x + margin,
					bounds.y + margin + vShift,
					bounds.width - margin * 2,
					bounds.height - margin * 2 - 1
			);
			
			Rectangle rectShifted = new Rectangle(
					bounds.x + margin + hotShift,
					bounds.y + margin + hotShift + vShift,
					bounds.width - margin*2,
					bounds.height - margin*2 - 1
			);
			
			// Draw tab shadow
			if (isHot) {
				gc.setForeground(shadowCol.get());
				gc.drawRectangle(rect);
			}
			
			// Draw tab background
			gc.setBackground(backCol.get());
			gc.fillRectangle(rectShifted);

			// Draw tab border
			gc.setForeground(borderCol.get());
			gc.drawRectangle(rectShifted);
			
			// Draw tab image
			Image image = item.getImage();
			int imgEndOffset = 2;
			if (image != null) {
				Rectangle imgBounds = image.getBounds();
				int hImgShift = 6;
				int vImgShift = (bounds.height - imgBounds.height) / 2;
				gc.drawImage(image,
						bounds.x + hImgShift + hotShift,
						bounds.y + vImgShift + hotShift + vShift
				);
				imgEndOffset = hImgShift + imgBounds.width;
			}
			
			// Draw tab text
			String text = item.getText();
			Point textExt = gc.textExtent(text);
			gc.setForeground(textCol.get());
			gc.drawString(text,
					bounds.x + imgEndOffset + margin + hotShift,
					bounds.y + (bounds.height - textExt.y) / 2 + hotShift + vShift,
					true
			);
			
			// Draw close button
			if (isHot && item.getShowClose()) {
				int x = bounds.x + bounds.width - 20;
				int y = (bounds.height - 9) / 2 + 1;
				int[] shape = {
						x,y, x+2,y, x+4,y+2, x+5,y+2, x+7,y, x+9,y,
						x+9,y+2, x+7,y+4, x+7,y+5, x+9,y+7, x+9,y+9,
						x+7,y+9, x+5,y+7, x+4,y+7, x+2,y+9, x,y+9,
						x,y+7, x+2,y+5, x+2,y+4, x,y+2
				};
				gc.setBackground(Col.WHITE.get());
				gc.fillPolygon(shape);
				gc.setForeground(Col.BLACK.get());
				gc.drawPolygon(shape);
			}
		}
	}

}
