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

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
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
		MainShellMaximized (false),
		UseOrOperator (true),
		PreferHtmlPreview (true),
		HighlightingEnabled (true),
		HideOnOpen (true),
		;

		public final Event<Boolean> evtChanged = new Event<Boolean> ();
		public final boolean defaultValue;
		private boolean value;

		Bool(boolean defaultValue) {
			value = this.defaultValue = defaultValue;
		}
		@SuppressAjWarnings
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
		IndexingErrorTable (200, 500),
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
			int colLength = columns.length;
			Util.checkThat(colLength > 0);
			if (colLength != value.length) {
				Util.checkThat(colLength == defaultValue.length);
				value = defaultValue;
			}
			
			for (int i = 0; i < colLength; i++) {
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
			int[] weights = sash.getWeights();
			assert weights.length > 0;
			if (weights.length != value.length) {
				assert weights.length == defaultValue.length;
				value = defaultValue;
			}
			for (int i = 0; i < weights.length; i++) {
				final int index = i;
				weights[i] = value[i];
				for (Control control : sash.getChildren())
					control.addControlListener(new ControlAdapter() {
						public void controlResized(ControlEvent e) {
							value[index] = sash.getWeights()[index];
							
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
			sash.setWeights(weights);
			
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
	public static enum Font implements Storable {
		// TODO now: update system fonts on SWT.Settings event

		/** A bold version of the system font; will be set during runtime. */
		SystemBold ("Times", 12, SWT.BOLD),

		/** A smaller version of the system font; will be set during runtime. */
		SystemSmall ("Times", 8, SWT.NORMAL),
		;

		private static Display display;
		private static Pattern fontPattern = Pattern.compile(
				"(.*)," + // Font name with whitespace
				"\\s*(\\d+)\\s*," + // Font height with whitespace
				"\\s*(\\d+)\\s*" // Font style with whitespace
		);

		public FontData defaultValue;
		private FontData value;
		private org.eclipse.swt.graphics.Font font;

		Font(String name, int height, int style) {
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
			return Joiner.on(", ").join(
					value.getName(),
					value.getHeight(),
					value.getStyle()
			);
		}

		/**
		 * Returns the <tt>Font</tt> object corresponding to this enumeration
		 * constant. It is disposed of automatically after the display is
		 * disposed. This method must be called from the GUI thread.
		 */
		public org.eclipse.swt.graphics.Font get() {
			if (display == null) {
				display = Display.getDefault();
				display.disposeExec(new Runnable() {
					public void run() {
						for (Font font : values())
							if (font.font != null)
								font.font.dispose();
					}
				});
				FontData fontData = display.getSystemFont().getFontData()[0];
				String name = fontData.getName();
				SystemBold.value.setName(name);
				SystemBold.defaultValue.setName(name);
				SystemSmall.value.setName(name);
				SystemSmall.defaultValue.setName(name);
				int height = fontData.getHeight();
				SystemBold.value.setHeight(height);
				SystemBold.defaultValue.setHeight(height);
				int smallHeight = Math.max(7, height - 5);
				SystemSmall.value.setHeight(smallHeight);
				SystemSmall.defaultValue.setHeight(smallHeight);
			}
			if (font == null)
				font = new org.eclipse.swt.graphics.Font(display, value);
			return font;
		}
	}

	@Description("# Color entries, consisting of red, green and blue values.")
	public enum Col implements Storable {
		;

		private static Display display;

		public RGB defaultValue;
		private RGB value;
		private Color color;

		Col(int red, int green, int blue) {
			value = defaultValue = new RGB(red, green, blue);
		}
		Col(float hue, float saturation, float brightness) {
			value = defaultValue= new RGB(hue, saturation, brightness);
		}
		public void load(String str) {
			int[] values = Util.toIntArray(str, new int[] {value.red, value.green, value.blue});
			value = new RGB(values[0], values[1], values[2]);
		}
		public String valueToString() {
			return Ints.join(", ", value.red, value.green, value.blue);
		}

		/**
		 * Returns the <tt>Color</tt> object corresponding to this enumeration
		 * constant. It is disposed of automatically after the display is
		 * disposed. This method must be called from the GUI thread.
		 */
		public Color get() {
			if (display == null) {
				display = Display.getDefault();
				display.disposeExec(new Runnable() {
					public void run() {
						for (Col col : values())
							if (col.color != null)
								col.color.dispose();
					}
				});
			}
			if (color == null)
				color = new Color(display, value);
			return color;
		}
	}

	private SettingsConf () {}
	
	public static String loadHeaderComment() throws IOException {
		URL url = SettingsConf.class.getResource("settings-conf-header.txt");
		return Resources.toString(url, Charsets.UTF_8);
	}

}
