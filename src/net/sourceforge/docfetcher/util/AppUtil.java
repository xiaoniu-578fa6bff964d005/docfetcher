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

package net.sourceforge.docfetcher.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.StackTraceWindow;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.sun.jna.platform.win32.Shell32Util;

/**
 * A container for various utility methods. These aren't members of the
 * {@link Util} package because they depend on some enum constants defined in
 * {@link Const} and {@link Msg}. The <code>Const</code> constants must have
 * been set before any method of this class can be called, otherwise an
 * <code>Exception</code> will be thrown. Setting the <code>Msg</code>
 * constants, on the other hand, is optional.
 * 
 * @author Tran Nam Quang
 */
public final class AppUtil {
	
	public static enum Const {
		/** This is the internal program name, not the name that can be changed by the user! */
		PROGRAM_NAME,
		PROGRAM_VERSION,
		PROGRAM_BUILD_DATE,
		USER_DIR_PATH,
		IS_PORTABLE,
		IS_DEVELOPMENT_VERSION,
		;
		
		private String value;
		
		@Nullable public String get() {
			return value;
		}
		
		public void set(@NotNull String value) {
			Util.checkNotNull(value);
			if (this.value != null)
				throw new UnsupportedOperationException("Constant cannot be set twice: " + this.name());
			if (this == IS_PORTABLE)
				this.value = String.valueOf(Boolean.parseBoolean(value));
			else
				this.value = value;
		}
		
		public void set(boolean b) {
			set(String.valueOf(b));
		}
		
		private boolean asBoolean() {
			return Boolean.parseBoolean(value);
		}
		
		/**
		 * Automatically initializes all constants. Should only be used for
		 * debugging. Calling this method repeatedly or after the constants have
		 * already been set manually will have no effect.
		 */
		public static void autoInit() {
			if (initialized)
				return;
			
			PROGRAM_NAME.set("DocFetcher");
			PROGRAM_VERSION.set("Unspecified");
			PROGRAM_BUILD_DATE.set("Unspecified");
			USER_DIR_PATH.set(Util.USER_DIR_PATH);
			IS_PORTABLE.set("true");
			IS_DEVELOPMENT_VERSION.set("true");
			
			for (Const c : Const.values())
				if (c.value == null)
					throw new IllegalStateException();
			
			initialized = true;
		}
		
		public static void clear() {
			for (Const c : Const.values())
				c.value = null;
			initialized = false;
		}
	}
	
	public static enum Msg {
		system_error ("System Error"),
		confirm_operation ("Confirm Operation"),
		invalid_operation ("Invalid Operation"),
		program_died_stacktrace_written (
				"This program just died! " +
				"The stacktrace below has been written to <a href=\"{0}\">{1}</a>."),
		program_running_launch_another (
				"It seems {0} is already running. " +
				"Do you want to launch another instance?"),
		;
		
		private String value;
		
		Msg(String defaultValue) {
			this.value = defaultValue;
		}
		
		@NotNull public String get() {
			return value;
		}
		
		public void set(@NotNull String value) {
			this.value = Util.checkNotNull(value);
		}
		
		/**
		 * Returns a string created from a <tt>java.text.MessageFormat</tt>
		 * with the given argument(s).
		 */
		private String format(Object... args) {
			return MessageFormat.format(value, args);
		}
	}
	
	private AppUtil() {}
	
	private static File appDataDir;
	private static Display display;
	private static boolean initialized = false;
	
	public static void setDisplay(@NotNull Display display) {
		Util.checkNotNull(display);
		Util.checkThat(AppUtil.display == null);
		AppUtil.display = display;
		
		display.disposeExec(new Runnable() {
			public void run() {
				AppUtil.display = null;
			}
		});
	}
	
	private static void checkConstInitialized() {
		if (! initialized) {
			for (Const value : Const.values()) {
				if (value.value == null)
					throw new IllegalStateException("Uninitialized constant: " + value.name());
			}
			initialized = true;
		}
	}
	
	private static void ensureNoDisplay() {
		if (display != null)
			throw new IllegalStateException("Display has already been initialized.");
	}
	
	private static void ensureDisplay() {
		if (display == null)
			throw new IllegalStateException("Display has not been initialized.");
	}
	
	/**
	 * Checks whether an instance of this program is already running. The check
	 * relies on a lockfile created in the temporary directory. The argument is
	 * the program name to be displayed in the confirmation dialog (see below).
	 * <p>
	 * The boolean return value should be interpreted as
	 * "proceed with running this instance?". More specifically:
	 * <ul>
	 * <li>If there is no other instance, true is returned.
	 * <li>If there is another instance, a confirmation dialog is shown, which
	 * asks the user whether this instance should be launched. If the user
	 * confirms, true is returned, otherwise false.
	 * </ul>
	 * The filename of the lockfile includes the username and the working
	 * directory, which means:
	 * <ul>
	 * <li>The same instance may be launched multiple times by different users.
	 * <li>Multiple instances from different locations can be run simultaneously
	 * by the same user.
	 * </ul>
	 * <p>
	 * <b>Note</b>: The message container must be loaded before calling this
	 * method, otherwise the confirmation dialog will show untranslated strings.
	 */
	// TODO doc: before calling this method, set USER_DIR_PATH, PROGRAM_NAME and all Msg enums
	public static boolean checkSingleInstance() {
		checkConstInitialized();
		ensureNoDisplay();

		/*
		 * The lockfile is created in the system's temporary directory to make
		 * sure it gets deleted after OS shutdown - neither File.deleteOnExit()
		 * nor a JVM shutdown hook can guarantee this.
		 * 
		 * The name of the lockfile includes the base64-encoded working
		 * directory, thus avoiding filename collisions if the same user runs
		 * multiple program instances from different locations. We'll also put
		 * in a SHA-1 digest of the working directory, just in case the
		 * base64-encoded working directory exceeds the system's allowed
		 * filename length limit, which we'll assume to be 255 characters.
		 * 
		 * Note that the included program name is also encoded as base64 since a
		 * developer might have (accidentally?) set a program name that contains
		 * special characters such as '/'.
		 */
		
		String dirPath = Const.USER_DIR_PATH.value;
		String shaDirPath = DigestUtils.shaHex(dirPath);
		String programName64 = encodeBase64(Const.PROGRAM_NAME.value);
		String username64 = encodeBase64(System.getProperty("user.name"));
		String dirPath64 = encodeBase64(dirPath);
		
		String lockname = String.format(
				".lock-%s-%s-%s-%s.", // dot at the end is intentional
				shaDirPath, programName64, username64, dirPath64
		);
		// Usual filename length limit is 255 characters
		lockname = lockname.substring(0, Math.min(lockname.length(), 250));
		File lockfile = new File(Util.TEMP_DIR, lockname);
		
		if (lockfile.exists()) {
			// Show message, ask whether to launch new instance or to abort
			Display display = new Display();
			Shell shell = new Shell(display);
			MessageBox msgBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL | SWT.PRIMARY_MODAL);
			msgBox.setText(Msg.confirm_operation.value);
			msgBox.setMessage(Msg.program_running_launch_another.format(Const.PROGRAM_NAME.value));
			int ans = msgBox.open();
			display.dispose();
			if(ans != SWT.OK)
				return false;
			/*
			 * If the user clicks OK, we'll take over the lockfile we found and
			 * delete it on exit. That means: (1) If there's another instance
			 * running, we'll wrongfully "steal" the lockfile from it. (2) If
			 * there's no other instance running (probably because it crashed or
			 * was killed by the user), we'll rightfully take over an orphaned
			 * lockfile. This behavior is okay, assuming the second case is more
			 * likely.
			 */
		} else {
			try {
				lockfile.createNewFile();
			} catch (IOException e) {
			}
		}
		lockfile.deleteOnExit();
		return true;
	}
	
	private static String encodeBase64(String input) {
		String encodedBytes = Base64.encodeBase64URLSafeString(input.getBytes());
		return new String(encodedBytes);
	}
	
	/**
	 * Returns a shell associated with this program. This method will first try
	 * to return the currently active shell. If no shell is active, it will try
	 * to return the first inactive shell. If there are no shells at all, null
	 * is returned.
	 * <p>
	 * This method should not be called from a non-GUI thread, and it should not
	 * be called before the first display is created.
	 */
	private static Shell getActiveShell() {
		ensureDisplay();
		Shell shell = display.getActiveShell();
		if (shell != null) return shell;
		Shell[] shells = display.getShells();
		if (shells.length != 0) return shells[0];
		return null;
	}
	
	/**
	 * Shows the given message in an error message box, with "System Error" as
	 * the shell title. If <tt>isSevere</tt> is true, an error icon is shown,
	 * otherwise a warning icon.
	 * <p>
	 * This method can be used before any GUI components are created, because it
	 * creates its own display and shell. If there is already a GUI,
	 * {@link #showErrorMsg} should be used instead.
	 */
	public static void showErrorOnStart(String message, boolean isSevere) {
		checkConstInitialized();
		ensureNoDisplay();
		
		Display display = new Display();
		Shell shell = new Shell(display);
		int style = SWT.OK;
		style |= isSevere ? SWT.ICON_ERROR : SWT.ICON_WARNING;
		MessageBox msgBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		msgBox.setText(Msg.system_error.value);
		msgBox.setMessage(message);
		msgBox.open();
		shell.dispose();
		display.dispose();
	}

	/**
	 * Shows the given message in a confirmation message box and returns the
	 * user's answer, either <tt>SWT.OK</tt> or <tt>SWT.CANCEL</tt>. If
	 * <tt>isSevere</tt> is true, a warning icon is shown, otherwise a question
	 * icon.
	 * <p>
	 * This method may be called from a non-GUI thread. It should not be called
	 * before the first shell is created.
	 */
	public static int showConfirmation(final String message, final boolean isSevere) {
		checkConstInitialized();
		ensureDisplay();
		class MyRunnable implements Runnable {
			private int answer = -1;
			public void run() {
				int style = SWT.OK | SWT.CANCEL;
				style |= isSevere ? SWT.ICON_WARNING : SWT.ICON_QUESTION;
				MessageBox msgBox = new MessageBox(getActiveShell(), style);
				msgBox.setText(Msg.confirm_operation.value);
				msgBox.setMessage(message);
				answer = msgBox.open();
			}
		}
		MyRunnable myRunnable = new MyRunnable();
		Util.runSwtSafe(display, myRunnable);
		return myRunnable.answer;
	}

	/**
	 * Shows the given message in a message box with an information icon. This
	 * method may be called from a non-GUI thread. It should not be called
	 * before the first shell is created.
	 */
	public static void showInfo(final String message) {
		checkConstInitialized();
		ensureDisplay();
		
		Util.runSwtSafe(display, new Runnable() {
			public void run() {
				MessageBox msgBox = new MessageBox(getActiveShell(),
						SWT.ICON_INFORMATION | SWT.OK);
				msgBox.setMessage(message);
				msgBox.open();
			}
		});
	}

	/**
	 * Shows the given message in an error message box. If
	 * <tt>errorNotWarning</tt> is true, an error icon is shown, otherwise a
	 * warning icon. If <tt>isUserError</tt> is true, the shell title is set to
	 * "Invalid Operation", otherwise "System Error".
	 * <p>
	 * This method may be called from a non-GUI thread. It should not be called
	 * before the first shell is created.
	 */
	public static void showError(	@NotNull final String message,
									final boolean errorNotWarning,
									final boolean isUserError) {
		checkConstInitialized();
		ensureDisplay();
		
		Util.runSwtSafe(display, new Runnable() {
			public void run() {
				int style = SWT.OK;
				style |= errorNotWarning ? SWT.ICON_ERROR : SWT.ICON_WARNING;
				MessageBox msgBox = new MessageBox(getActiveShell(), style);
				msgBox.setText(isUserError ? Msg.invalid_operation.value
						: Msg.system_error.value);
				msgBox.setMessage(message);
				msgBox.open();
			}
		});
	}

	/**
	 * Prints the stacktrace to {@link System.err} and to a stacktrace file. In
	 * addition to that, the stacktrace is displayed in an error window. The
	 * printouts for the file and the error window are prepended with some
	 * useful debug information about the program.
	 * <p>
	 * This method creates its own display, and should therefore be called
	 * either before the application's display has been created, or after the
	 * application's display has been disposed. In between,
	 * {@link #showStackTrace} should be used instead.
	 */
	public static void showStackTraceInOwnDisplay(Throwable throwable) {
		checkConstInitialized();
		ensureNoDisplay();
		
		Display display = new Display();
		showStackTrace(display, throwable);
		display.dispose();
	}

	/**
	 * Prints the stacktrace to {@link System.err} and to a stacktrace file. In
	 * addition to that, the stacktrace is displayed in an error window. The
	 * printouts for the file and the error window are prepended with some
	 * useful debug information about the program.
	 * <p>
	 * It is safe to call this method from a non-GUI thread. The method should
	 * not be called before the first display has been created. In the latter case
	 * {@link #showStackTraceOnStart} should be used instead.
	 */
	public static void showStackTrace(Throwable throwable) {
		checkConstInitialized();
		ensureDisplay();
		
		showStackTrace(Display.getDefault(), throwable);
	}

	@SuppressAjWarnings
	private static void showStackTrace(final Display display, final Throwable throwable) {
		// Print stacktrace to System.err
		throwable.printStackTrace();

		// Prepend useful program info to the stacktrace
		StringBuilder sb = new StringBuilder();
		sb.append("program.name=" + Const.PROGRAM_NAME.value + "\n");
		sb.append("program.version=" + Const.PROGRAM_VERSION.value + "\n");
		sb.append("program.build=" + Const.PROGRAM_BUILD_DATE.value + "\n");
		String[] keys = {
				"java.runtime.name",
				"java.runtime.version",
				"java.version",
				"os.arch",
				"os.name",
				"os.version",
				"user.language"
		};
		for (String key : keys)
			sb.append(key + "=" + System.getProperty(key) + "\n");

		// Get stacktrace as string
		StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		sb.append(writer.toString());
		final String trace = sb.toString();

		// Write stacktrace to file
		String timestamp = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date());
		String traceFilename = "stacktrace_" + timestamp + ".txt";
		final File traceFile = new File(getAppDataDir(), traceFilename);
		try {
			Files.write(trace, traceFile, Charsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace(); // We'll give up here
		}

		// Show stacktrace in error window
		Util.runSwtSafe(display, new Runnable() {
			public void run() {
				StackTraceWindow window = new StackTraceWindow(display);
				window.setTitle(throwable.getClass().getSimpleName());
				String path = Util.getSystemAbsPath(traceFile);
				String msg = Msg.program_died_stacktrace_written.format(path, path);
				window.setText(msg);
				Image icon = display.getSystemImage(SWT.ICON_WARNING);
				window.setTitleImage(icon);
				window.setStackTrace(trace);
				window.open();
			}
		});
	}

	/**
	 * Returns a directory where the program may store data. The directory is
	 * created if necessary. The rules for choosing the directory are as
	 * follows:
	 * <p>
	 * <ul>
	 * <li>If the {@code portable} flag was set, the current working directory
	 * is returned.
	 * <li>If the {@code is development version} flag was set, the "bin"
	 * directory under the current working directory is returned.
	 * <li>Otherwise, the returned directory is platform-dependent: On Windows,
	 * the application data folder + program name is returned, on Linux the home
	 * folder + dot + lowercase program name.
	 * </ul>
	 */
	public static File getAppDataDir() {
		checkConstInitialized();
		
		if (appDataDir != null)
			return appDataDir; // Return cached value
		
		String programName = Const.PROGRAM_NAME.value;
		File appDataDir = null;
		if (Const.IS_DEVELOPMENT_VERSION.asBoolean()) {
			// The development flag has higher priority than the portable flag
			appDataDir = new File("bin");
		}
		else if (Const.IS_PORTABLE.asBoolean()) {
			appDataDir = new File(Const.USER_DIR_PATH.value);
		}
		else if (Util.IS_WINDOWS) {
			// Windows 7/Vista: C:\Users\<UserName>\AppData\<ProgramName>
			// Windows XP/2000: C:\Documents and Settings\<UserName>\Application Data\<ProgramName>
			String winAppData = System.getenv("APPDATA");
			if (winAppData == null)
				/*
				 * Bug #2812637: The previous System.getenv("APPDATA") call
				 * returns null if DocFetcher is started as an alternative user
				 * via the executable's "Run as..." context menu entry. If this
				 * happens, we'll have to fall back to this JNA-based
				 * workaround.
				 */
				winAppData = Shell32Util.getFolderPath(0x001a); // CSIDL_APPDATA = 0x001a
			if (winAppData == null)
				throw new IllegalStateException("Cannot find application data folder.");
			appDataDir = new File(winAppData, programName);
		}
		else if (Util.IS_LINUX) {
			// /home/<UserName>/.<LowerCaseProgramName>
			appDataDir = new File(Util.USER_HOME_PATH, "." + programName.toLowerCase());
		}
		else {
			// TODO mac: implement this on the Mac
			throw new IllegalStateException();
		}
		
		appDataDir.mkdirs();
		AppUtil.appDataDir = appDataDir; // Store value in cache
		return appDataDir;
	}
	
	public static boolean isPortable() {
		checkConstInitialized();
		return Const.IS_PORTABLE.asBoolean();
	}

}
