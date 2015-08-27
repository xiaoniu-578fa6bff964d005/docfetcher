/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.enums;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.util.ConfLoader.Description;
import net.sourceforge.docfetcher.util.ConfLoader.Storable;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.Immutable;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.common.primitives.Ints;

/**
 * This class handles the storage and retrieval of application-wide internal
 * program preferences and allows type safe access to them via nested enums. The
 * default values of the preferences entries are hardcoded so as to avoid
 * program corruption caused by manipulation of the preferences file by users.
 * <p>
 * New entries can be added by adding enum members to the nested enums.
 * CamelCase names are to be preferred to UPPERCASE names since the former make
 * the preferences file more readable. Duplicate names (e.g. Pref.Bool.Test and
 * Pref.Int.Test) are not supported and should be avoided.
 * <p>
 * New enums (not enum <em>members</em>) must implement the
 * <code>Storable</code> interface and have a <code>Description</code>
 * annotation; everything else is handled automatically via reflection.
 *
 * @author Tran Nam Quang
 */
public final class SettingsConf {

	// TODO pre-release: remove unused entries

	@Description("# Boolean entries. Allowed values: true, false")
	public static enum Bool implements Storable {
		ShowManualOnStartup (true),
		UseOrOperator (true),
		HideOnOpen (false), // Default must be 'false' since system tray not supported on Ubuntu Unity
		ClearSearchHistoryOnExit (false),
		ResetLocationFilterOnExit (true),
		HotkeyEnabled (true),

		ShowFilterPanel (true),
		ShowPreviewPanel (true),
		ShowPreviewPanelAtBottom (true),

		FilesizeFilterMaximized (true),
		TypesFilterMaximized (false),
		LocationFilterMaximized (false),

		MainShellMaximized (false),
		PreferHtmlPreview (true),
		HighlightingEnabled (true),
		ShowRelativePathsMessage (true),
		AutoScrollToFirstMatch (true),
		CloseToTray (false), // Default must be 'false' since system tray not supported on Ubuntu Unity
		AllowOnlyOneInstance (false),
		ShowJavaVersion (false),
		;

		public final Event<Boolean> evtChanged = new Event<Boolean> ();
		public final boolean defaultValue;
		private boolean value;

		Bool(boolean defaultValue) {
			value = this.defaultValue = defaultValue;
		}
		public boolean get() {
			return value;
		}
		public void set(boolean value) {
			if (this.value == value) return;
			this.value = value;
			evtChanged.fire(value);
		}
		public void load(String str) {
			value = Boolean.parseBoolean(str);
		}
		public String valueToString() {
			return Boolean.toString(value);
		}
		public void bindMaximized(final Shell shell) {
			shell.setMaximized(value);
			shell.addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					value = shell.getMaximized();
				}
			});
		}
	}

	@Description("# Integer entries.")
	public static enum Int implements Storable {
		FilterPanelWidth (250),
		;

		public final Event<Integer> evtChanged = new Event<Integer> ();
		public final int defaultValue;
		private int value;

		Int(int defaultValue) {
			value = this.defaultValue = defaultValue;
		}
		public int get() {
			return value;
		}
		public void set(int value) {
			if (this.value == value) return;
			this.value = value;
			evtChanged.fire(value);
		}
		public void load(String str) {
			value = Util.toInt(str, value);
		}
		public String valueToString() {
			return Integer.toString(value);
		}
	}

	@Description("# String entries. All values allowed, except for \\, \\t, \\n," +
			"\r\n# \\r, \\f, =, :, #, ! and leading whitespace, which must be" +
			"\r\n# escaped by a preceding \\.")
	public static enum Str implements Storable {
		LastIndexedFolder (Util.USER_HOME_PATH),
		LastIndexedPSTFile (""),
		;

		public final Event<String> evtChanged = new Event<String> ();
		public final String defaultValue;
		private String value;

		Str(String defaultValue) {
			value = this.defaultValue = defaultValue;
		}
		public String get() {
			return value;
		}
		public void set(String value) {
			if (this.value == value) return;
			this.value = value;
			evtChanged.fire(value);
		}
		public void load(String str) {
			value = str;
		}
		public String valueToString() {
			return value;
		}
	}

	@Description("# Comma-separated lists of integers.")
	public static enum IntArray implements Storable {
		PreviewHighlighting (255, 255, 0),
		HotkeyToFront (SWT.CTRL, SWT.F8),

		FilterSash (1, 1),
		RightSashHorizontal (1, 1),
		RightSashVertical (1, 1),
		;

		public final Event<int[]> evtChanged = new Event<int[]> ();
		public final int[] defaultValue;
		private int[] value;

		IntArray(int... defaultValue) {
			value = this.defaultValue = defaultValue;
		}
		public int[] get() {
			return value;
		}
		public void set(int... value) {
			if (Arrays.equals(this.value, value)) return;
			this.value = value;
			evtChanged.fire(value);
		}
		public void load(String str) {
			value = Util.toIntArray(str, value);
		}
		public String valueToString() {
			return Ints.join(", ", value);
		}
	}

	@Description("# Semicolon-separated lists of strings.")
	public static enum StrList implements Storable {
		SearchHistory,
		;

		public final Event<List<String>> evtChanged = new Event<List<String>> ();
		@Immutable public final List<String> defaultValue;
		private List<String> value;

		StrList(String... defaultValue) {
			value = this.defaultValue = Arrays.asList(defaultValue);
		}
		@Immutable
		public List<String> get() {
			return Collections.unmodifiableList(value);
		}
		public void set(String... value) {
			if (Util.equals(this.value, value))
				return;
			this.value = Arrays.asList(value);
			evtChanged.fire(this.value);
		}
		public void set(List<String> value) {
			if (this.value.equals(value))
				return;
			this.value = value;
			evtChanged.fire(value);
		}
		public void load(String str) {
			value = Util.decodeStrings(';', str);
		}
		public String valueToString() {
			return Util.encodeStrings(";", value);
		}
	}

	@Description("# Window dimensions: x, y, width and height. If x < 0 or" +
			"\r\n# y < 0, the window is centered relative to the screen or to" +
			"\r\n# its parent window.")
	public static enum ShellBounds implements Storable {
		MainWindow (-1, -1, 640, 480),
		IndexingDialog (-1, -1, 450, 500),
		FileExtensionChooser (-1, -1, 300, 450),
		PreferencesDialog (-1, -1, 500, 400),
		;

		public final int[] defaultValue;
		private int[] value;

		ShellBounds(int x, int y, int width, int height) {
			value = defaultValue = new int[] {x, y, width, height};
		}
		public void bind(final Shell shell) {
			if (value[0] < 0 || value[1] < 0)
				Util.setCenteredBounds(shell, value[2], value[3]);
			else
				shell.setBounds(value[0], value[1], value[2], value[3]);
			shell.addControlListener(new ControlAdapter() {
				public void controlMoved(ControlEvent e) {
					if (shell.getMaximized()) return;
					Point pos = shell.getLocation();
					value[0] = pos.x;
					value[1] = pos.y;
				}
				public void controlResized(ControlEvent e) {
					if (shell.getMaximized()) return;
					if (ProgramConf.Bool.FixWindowSizes.get()) return;
					Point size = shell.getSize();
					value[2] = size.x;
					value[3] = size.y;
				}
			});
		}
		public void load(String str) {
			value = Util.toIntArray(str, value);
		}
		public String valueToString() {
			return Ints.join(", ", value);
		}
	}

	@Description("# Comma-separated lists of table column widths.")
	public static enum ColumnWidths implements Storable {
		ResultPanel (250, 75, 75, 200, 75, 350, 100, 100),
		IndexingErrorTable (100, 100, 500),
		PatternTable (200, 75, 75),
		;

		private final Event<Table> evtChanged = new Event<Table>();
		public final int[] defaultValue;
		private int[] value;

		ColumnWidths(int... defaultValue) {
			value = this.defaultValue = defaultValue;
		}

		/**
		 * Binds the enumeration's values to the column widths of the given
		 * table, i.e. the column widths are initialized with the stored values
		 * and the values are updated when the column widths change.
		 * <p>
		 * This method supports binding multiple tables to the same enumeration:
		 * If multiple tables are bound, the column widths are synchronized
		 * across all bound tables.
		 */
		public void bind(@NotNull final Table table) {
			final TableColumn[] columns = table.getColumns();
			int columnCount = columns.length;
			Util.checkThat(columnCount > 0);
			if (columnCount != value.length) {
				Util.checkThat(columnCount == defaultValue.length);
				value = defaultValue;
			}

			for (int i = 0; i < columnCount; i++) {
				final TableColumn col = columns[i];
				final int index = i;
				col.setWidth(value[i]);
				col.addControlListener(new ControlAdapter() {
					public void controlResized(ControlEvent e) {
						value[index] = col.getWidth();
						evtChanged.fire(table);
					}
				});
			}

			// Update column widths if they have been changed in other tables
			final Event.Listener<Table> changeListener = new Event.Listener<Table>() {
				public void update(Table eventData) {
					if (eventData == table)
						return;
					Util.checkThat(columns.length == table.getColumnCount());
					for (int i = 0; i < columns.length; i++)
						columns[i].setWidth(value[i]);
				}
			};
			evtChanged.add(changeListener);
			table.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					evtChanged.remove(changeListener);
				}
			});
		}
		public void load(String str) {
			value = Util.toIntArray(str, value);
		}
		public String valueToString() {
			return Ints.join(", ", value);
		}
	}

	@Description("# Column orderings.")
	public static enum ColumnOrder implements Storable {
		ResultPanelColumnOrder (),
		;

		public final int[] defaultValue;
		private int[] value;

		ColumnOrder(int... defaultValue) {
			value = this.defaultValue = defaultValue;
		}
		public void load(String str) {
			value = Util.toIntArray(str, value);
		}
		public String valueToString() {
			return Ints.join(", ", value);
		}

		public void bind(@NotNull final Table table) {
			final TableColumn[] columns = table.getColumns();
			int columnCount = columns.length;
			Util.checkThat(columnCount > 0);
			if (columnCount != value.length)
				value = defaultValue;

			ControlListener columnListener = new ControlAdapter() {
				public void controlMoved(ControlEvent e) {
					value = table.getColumnOrder();
				}
			};
			for (TableColumn column : columns) {
				column.setMoveable(true);
				column.addControlListener(columnListener);
			}

			if (table.getColumnCount() == value.length)
				table.setColumnOrder(value);
		}
	}

	@Description("# Comma-separated lists of sash weights.")
	public static enum SashWeights implements Storable {
		ProgressPanel (2, 1),
		;

		private final Event<SashForm> evtChanged = new Event<SashForm>();
		public final int[] defaultValue;
		private int[] value;

		SashWeights(int... defaultValue) {
			value = this.defaultValue = defaultValue;
		}

		/**
		 * Binds the enumeration's values to the weights of the given sash form,
		 * i.e. the sash weights are initialized with the stored values and the
		 * values are updated when the sash weights change.
		 * <p>
		 * This method supports binding multiple sash forms to the same
		 * enumeration: If multiple sash forms are bound, the sash weights are
		 * synchronized across all bound sash forms.
		 */
		public void bind(@NotNull final SashForm sash) {
			Control[] children = sash.getChildren();
			assert children.length > 0;
			if (children.length != value.length) {
				assert children.length == defaultValue.length;
				value = defaultValue;
			}
			sash.setWeights(value);

			for (Control control : children) {
				control.addControlListener(new ControlAdapter() {
					public void controlResized(ControlEvent e) {
						value = sash.getWeights();

						/*
						 * The event must be fired with asyncExec, otherwise
						 * we'll get some nasty visual artifacts.
						 */
						Util.runAsyncExec(sash, new Runnable() {
							public void run() {
								evtChanged.fire(sash);
							}
						});
					}
				});
			}

			// Update sash weights if they have been changed in other sash forms
			final Event.Listener<SashForm> changeListener = new Event.Listener<SashForm>() {
				public void update(SashForm eventData) {
					if (eventData == sash)
						return;
					if (Arrays.equals(sash.getWeights(), value))
						return;
					sash.setWeights(value);
				}
			};
			evtChanged.add(changeListener);
			sash.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					evtChanged.remove(changeListener);
				}
			});
		}
		public void load(String str) {
			value = Util.toIntArray(str, value);
		}
		public String valueToString() {
			return Ints.join(", ", value);
		}
	}

	@Description("# Font entries, consisting of font name, height and style.")
	public static enum FontDescription implements Storable {
		PreviewWindows ("Verdana", 10, SWT.NORMAL),
		PreviewLinux ("Sans", 10, SWT.NORMAL),
		PreviewMacOsX ("Helvetica", 12, SWT.NORMAL),
		PreviewMonoWindows ("Courier New", 10, SWT.NORMAL),
		PreviewMonoLinux ("Monospace", 10, SWT.NORMAL),
		PreviewMonoMacOsX ("Monaco", 10, SWT.NORMAL),
		;

		private static Pattern fontPattern = Pattern.compile(
				"(.*)," + // Font name with whitespace
				"\\s*(\\d+)\\s*," + // Font height with whitespace
				"\\s*(\\d+)\\s*" // Font style with whitespace
		);

		public final Event<Void> evtChanged = new Event<Void>();

		public final FontData defaultValue;
		private FontData value;

		FontDescription(String name, int height, int style) {
			value = defaultValue = new FontData(name, height, style);
		}
		public void load(String str) {
			Matcher matcher = fontPattern.matcher(str);
			if (! matcher.matches()) return;
			value = new FontData(
					matcher.group(1).trim(),
					Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(3))
			);
		}
		public String valueToString() {
			// The SWT API discourages accessing the FontData fields
			return Util.join(", ",
					value.getName(),
					value.getHeight(),
					value.getStyle()
			);
		}
		// Should only be called from within the SWT thread
		// Caller is responsible for disposing the returned font
		@NotNull
		public Font createFont() {
			return new Font(Display.getDefault(), value);
		}
		public void set(@NotNull FontData fontData) {
			Util.checkNotNull(fontData);
			value = fontData;
			evtChanged.fire(null);
		}
		@NotNull
		public FontData createFontData() {
			return new FontData(
				value.getName(), value.getHeight(), value.getStyle());
		}
		@NotNull
		public FontData createDefaultFontData() {
			return new FontData(
				defaultValue.getName(), defaultValue.getHeight(),
				defaultValue.getStyle());
		}
	}

	private SettingsConf () {}

	public static String loadHeaderComment() throws IOException {
		URL url = SettingsConf.class.getResource("settings-conf-header.txt");
		return Resources.toString(url, Charsets.UTF_8);
	}

}
