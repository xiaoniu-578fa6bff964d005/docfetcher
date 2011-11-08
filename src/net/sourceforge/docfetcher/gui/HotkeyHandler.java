/*******************************************************************************
 * Copyright (c) 2008 Tonio Rush.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tonio Rush - initial API and implementation
 *    Tran Nam Quang - removed some external dependencies; minor tweaks
 *******************************************************************************/

package net.sourceforge.docfetcher.gui;

import java.io.File;

import jxgrabkey.HotkeyConflictException;
import jxgrabkey.JXGrabKey;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Event.Listener;
import net.sourceforge.docfetcher.util.Util;

import com.melloware.jintellitype.JIntellitype;

/**
 * Handler for hotkeys.
 *
 * @author Tonio Rush
 * @author Tran Nam Quang
 */
public final class HotkeyHandler {
	
	/**
	 * This event is fired when the hotkey is pressed. Not that the listeners
	 * will be called from a non-GUI thread.
	 */
	public final Event<Void> evtHotkeyPressed = new Event<Void> ();
	
	/**
	 * This event is fired when the HotkeyHandler fails to register a hotkey
	 * because it is already registered by another program. Currently, this can
	 * only happen on Linux.
	 * <p>
	 * The integer array contains the state mask and the key this class tried to
	 * register.
	 */
	public final Event<int[]> evtHotkeyConflict = new Event<int[]> ();

	private static final int HOTKEY_ID = 1;

	private final HotkeyListenerImpl implementation;

	/**
	 * Installs a listener on a global hotkey. The hotkey is registred in
	 * <tt>SettingsConf.IntArray.HotKeyToFront</tt>.
	 * <p>
	 * This constructor will throw an <tt>UnsupportedOperationException</tt> if
	 * called on an unsupported platform, e.g. Mac OS X.
	 */
	public HotkeyHandler() {
		if (Util.IS_WINDOWS)
			implementation = new HotkeyListenerWindowsImpl();
		else if (Util.IS_LINUX)
			implementation = new HotkeyListenerLinuxImpl();
		else
			throw new UnsupportedOperationException();

		implementation.initialize(this);

		SettingsConf.IntArray.HotkeyToFront.evtChanged.add(new Listener<int[]>() {
			public void update(int[] eventData) {
				implementation.unregisterHotkey(HOTKEY_ID);
				
				if (SettingsConf.Bool.HotkeyEnabled.get())
					implementation.registerHotkey(HOTKEY_ID, eventData[0], eventData[1]);
			}
		});
		
		SettingsConf.Bool.HotkeyEnabled.evtChanged.add(new Event.Listener<Boolean> () {
			public void update(Boolean eventData) {
				if (eventData) {
					/*
					 * The hotkey conflict event is temporarily disabled here
					 * because we don't want to display an error message when an
					 * invalid hotkey is re-registered.
					 */
					evtHotkeyConflict.setEnabled(false);
					implementation.registerHotkey(HOTKEY_ID,
						SettingsConf.IntArray.HotkeyToFront.get()[0],
						SettingsConf.IntArray.HotkeyToFront.get()[1]);
					evtHotkeyConflict.setEnabled(true);
				}
				else
					implementation.unregisterHotkey(HOTKEY_ID);
			}
		});
	}
	
	/**
	 * Registers the hotkey.
	 */
	public void registerHotkey() {
		if (SettingsConf.Bool.HotkeyEnabled.get())
			implementation.registerHotkey(HOTKEY_ID,
				SettingsConf.IntArray.HotkeyToFront.get()[0],
				SettingsConf.IntArray.HotkeyToFront.get()[1]);
	}
	
	/**
	 * Unregisters the hotkey.
	 */
	public void shutdown() {
		implementation.unregisterHotkey(HOTKEY_ID);
		implementation.shutdown();
	}

	private void onHotKey(int hotkey_id) {
		if (hotkey_id != HOTKEY_ID)
			return;
		evtHotkeyPressed.fire(null);
	}
	
	private interface HotkeyListenerImpl {
		public void initialize(HotkeyHandler listener);
		public void registerHotkey(int id, int mask, int key);
		public void unregisterHotkey(int id);
		public void shutdown();
	}

	/**
	 * Windows implementation using JIntellitype.
	 */
	private final class HotkeyListenerWindowsImpl implements HotkeyListenerImpl {
		public void initialize(final HotkeyHandler listener) {
			boolean isDev = SystemConf.Bool.IsDevelopmentVersion.get();
			int arch = Util.IS_64_BIT_JVM ? 64 : 32;
			
			String libPath = isDev ? "lib/jintellitype" : "lib";
			libPath = String.format("%s/JIntellitype%d.dll", libPath, arch);
			JIntellitype.setLibraryLocation(libPath);
			
			JIntellitype.getInstance().addHotKeyListener(
				new com.melloware.jintellitype.HotkeyListener() {
					public void onHotKey(int hotkey_id) {
						listener.onHotKey(hotkey_id);
					}
				});
		}

		public void registerHotkey(int id, int mask, int key) {
			JIntellitype.getInstance().registerSwingHotKey(id,
					KeyCodeTranslator.translateSWTModifiers(mask),
					KeyCodeTranslator.translateSWTKey(key));
		}

		public void unregisterHotkey(int id) {
			JIntellitype.getInstance().unregisterHotKey(id);
		}

		public void shutdown() {
			JIntellitype.getInstance().cleanUp();
		}
	}

	/**
	 * Linux implementation using JXGrabKey.
	 */
	private final class HotkeyListenerLinuxImpl implements HotkeyListenerImpl {
		public void initialize(final HotkeyHandler listener) {
			boolean isDev = SystemConf.Bool.IsDevelopmentVersion.get();
			int arch = Util.IS_64_BIT_JVM ? 64 : 32;
			
			String libPath = isDev ? "lib/jxgrabkey" : "lib";
			libPath = String.format("%s/libJXGrabKey%d.so", libPath, arch);
			System.load(Util.getAbsPath(new File(libPath)));
			
			JXGrabKey.getInstance().addHotkeyListener(new jxgrabkey.HotkeyListener(){
				public void onHotkey(int hotkey_id) {
					listener.onHotKey(hotkey_id);
				}
	        });
		}

		public void registerHotkey(int id, int mask, int key) {
			try {
				JXGrabKey.getInstance().registerAwtHotkey(id,
						KeyCodeTranslator.translateSWTModifiers(mask),
						KeyCodeTranslator.translateSWTKey(key));
			}
			catch (HotkeyConflictException e) {
				evtHotkeyConflict.fire(new int[] {mask, key});
			}
		}
		
		public void unregisterHotkey(int id) {
			JXGrabKey.getInstance().unregisterHotKey(id);
		}

		public void shutdown() {
	        JXGrabKey.getInstance().cleanUp();
		}
	}
	
}
