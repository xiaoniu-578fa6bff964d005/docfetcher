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

package net.sourceforge.docfetcher.enums;

import java.text.MessageFormat;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

public enum Msg {
	
	// General
	system_error (
		"System Error",
		"Window title for a message box that displays an error."),
	confirm_operation (
		"Confirm Operation",
		"Window title for a message box that asks the user to confirm an " +
		"operation that is about to be run."),
	invalid_operation (
		"Invalid Operation",
		"Window title for a message box that informs the user that he/she is " +
		"not allowed to run a certain operation."),
	ok ("&OK",
		"Label for the 'OK' button of a message box. " +
		"The '&' character is applied to the character after it, and in this " +
		"case indicates that the user can activate the button by pressing Alt + O."),
	cancel ("&Cancel",
		"Label for the 'Cancel' button of a message box. " +
		"The '&' character is applied to the character after it, and in this " +
		"case indicates that the user can activate the button by pressing Alt + C."),
	close ("&Close",
		"Label for the 'Close' button of a dialog. " +
		"The '&' character is applied to the character after it, and in this " +
		"case indicates that the user can activate the button by pressing Alt + C."),
	hotkey_in_use (
		"The hotkey {0} DocFetcher tried to register seems to be already in use. " +
		"Please set an unused hotkey in the preferences.",
		"This message is shown to the user when the program failed to " +
		"register a global hotkey on startup."),
	report_bug (
		"Ooops! This program just died! " +
		"Please help us to fix this problem by " +
		"posting the stacktrace below on our " +
		"<a href=\"http://sourceforge.net/apps/mediawiki/docfetcher/index.php?title=Bug_Reports\">bug tracker</a> (no registration required).\n\n" +
		"The stacktrace has been written to:\n{0}.",
		//
		"This message is shown when the program crashes. Below the message, " +
		"there will be a detailed error report known as the 'stacktrace'. The " +
		"message contains a link to our bug tracker at sourceforge.net, and " +
		"it also says that the stacktrace has been written to a file, whose " +
		"path will be inserted at the {0} slot."),
	program_running_launch_another (
		"It seems {0} is already running. " +
		"Do you want to launch another instance?",
		//
		"The program has been started, and it has detected that another " +
		"instance of it is already running. Thus, it asks the user whether " +
		"he/she really wants to run both instances at the same time. The name " +
		"of the program will be inserted in the {0} slot - this is usually " +
		"'DocFetcher'."),
	file_not_found (
		"File not found:",
		"An error message that is shown when the program fails to find a " +
		"certain file. The path of the file will be appended to the message."),
	folder_not_found (
		"Folder not found:",
		"An error message that is shown when the program fails to find a " +
		"certain folder. The path of the file will be inserted into the {0} slot."),
	file_or_folder_not_found (
		"File or folder not found:",
		"An error message that is shown when the program fails to find a " +
		"certain file or folder. The path of the file/folder will be " +
		"inserted into the {0} slot."),
	files_or_folders_not_found (
		"Files or folders not found:",
		"An error message that is shown when the program fails to find " +
		"certain files or folders. The path of the files and folders will be " +
		"inserted into the {0} slot."),
	missing_image_files (
		"Missing image files:",
		"Error message shown on startup if some of the program's image " +
		"files are missing. The missing files will be listed below this message."),
	entries_missing (
		"The following entries in '{0}' are missing or have invalid values:",
		"Error message shown on startup if some entries in a properties file " +
		"are mssing or invalid. The name of the properties file will be inserted into " +
		"the {0} slot. The missing entries will be listed after this message."),
	
	// Filter panel
	min_max_filesize (
		"Minimum / Maximum Filesize",
		"Label for the filter control on the left of the GUI that allows the " +
		"user to filter the search results by filesize."),
	document_types (
		"Document Types",
		"Label for the filter control on the left of the GUI that allows the " +
		"user to filter the search results by document type."),
	search_scope ("Search Scope",
		"Label for the control on the left of the GUI that allows the user " +
		"to filter the search results by location, and to perform various " +
		"index-related operations, such as creating new indexes."),
	
	// File types
	filetype_abi ("AbiWord (abw, abw.gz, zabw)", Comments.filetype),
	filetype_chm ("MS Compiled HTML Help (chm)", Comments.filetype),
	filetype_doc ("MS Word (doc)", Comments.filetype),
	filetype_xls ("MS Excel (xls)", Comments.filetype),
	filetype_ppt ("MS Powerpoint (ppt)", Comments.filetype),
	filetype_vsd ("MS Visio (vsd)", Comments.filetype),
	filetype_docx ("MS Word 2007 (docx, docm)", Comments.filetype),
	filetype_xlsx ("MS Excel 2007 (xlsx, xlsm)", Comments.filetype),
	filetype_pptx ("MS Powerpoint 2007 (pptx, pptm)", Comments.filetype),
	filetype_html ("HTML (html, htm, ..)", Comments.filetype),
	filetype_odt ("OpenOffice.org Writer (odt, ott)", Comments.filetype),
	filetype_ods ("OpenOffice.org Calc (ods, ots)", Comments.filetype),
	filetype_odg ("OpenOffice.org Draw (odg, otg)", Comments.filetype),
	filetype_odp ("OpenOffice.org Impress (odp, otp)", Comments.filetype),
	filetype_pdf ("PDF Document (pdf)", Comments.filetype),
	filetype_rtf ("Rich Text Format (rtf)", Comments.filetype),
	filetype_svg ("Scalable Vector Graphics (svg)", Comments.filetype),
	filetype_txt ("Plain Text", Comments.filetype),
	
	// Search scope context menu entries
	create_index_from (
		"Create Index From",
		Comments.searchScopeEntry +	" It contains various submenu entries, e.g. " +
		"Folder, Archive, Outlook PST. The submenu entries would look like this " +
		"to the user: 'Create Index From > Folder', 'Create Index From > Archive', " +
		"and so on."),
	folder ("Folder...",
		Comments.createIndexFromEntry),
	archive ("Archive...",
		Comments.createIndexFromEntry),
	outlook_pst ("Outlook PST...",
		Comments.createIndexFromEntry),
	clipboard ("Clipboard...\tCtrl+V",
		Comments.createIndexFromEntry + " The '\tCtrl+V' is a keyboard shortcut " +
		"and might need translation. For example, in German it would be '\tStrg+V'."),
	clipboard_macosx ("Clipboard...\t\u2318V",
		Comments.createIndexFromEntry + " The '\t\u2318V' is a keyboard shortcut, " +
		"with 'u2318' being a Mac OS X specific modifier key known as " +
		"'Command'. Do not translate it."),
	update_index (
		"Update Index...\tF5",
		Comments.searchScopeEntry +
		" Translate the keyboard shortcut if appropriate."),
	rebuild_index (
		"Rebuild Index...",
		Comments.searchScopeEntry),
	remove_index (
		"Remove Index\tDelete",
		Comments.searchScopeEntry +
		" Translate the 'Delete' keyboard shortcut if appropriate."),
	remove_sel_indexes (
		"Remove selected indexes?",
		"This confirmation message is shown when the user is about to " +
		"remove an index."),
	remove_orphaned_indexes (
		"Remove Orphaned Indexes",
		Comments.searchScopeEntry + " It allows the user to remove all the " +
		"indexes whose associated document folders have been deleted."),
	remove_orphaned_indexes_msg (
		"Remove all indexes whose document folders are missing?",
		"This confirmation message is shown when the user is about to remove " +
		"all orphaned indexes."),
	check_all ("Check All",
		Comments.searchScopeEntry +
		" It allows the user to mark the checkboxes of all folders and " +
		"subfolders in the 'Search Scope' control."),
	uncheck_all ("Uncheck All",
		Comments.searchScopeEntry +
		" It allows the user to unmark the checkboxes of all folders and " +
		"subfolders in the 'Search Scope' control."),
	check_single (
		"Toggle Individual Check State",
		Comments.searchScopeEntry +
		" The purpose of this entry is as follows: If the user simply clicks " +
		"on a checkbox in the 'Search Scope' control, this will change not " +
		"only the check state of the associated folder, but also the check " +
		"states of all of the folder's subfolders. This menu entry on the " +
		"other hand allows the user to change the check state of a folder " +
		"*without* changing the check states of the folder's subfolders."),
	open_folder ("Open Folder",
		Comments.searchScopeEntry + " It allows the user to open the selected " +
		"folder in the system's file manager, e.g. Windows Explorer."),
	list_docs ("List Documents",
		Comments.searchScopeEntry + " It allows the user to show the contents " +
		"of the selected folder in the search results pane."),
	
	// Various GUI controls
	search ("Search",
		"Label of the 'Search' button right next to the search field."),
	open_manual ("Open Manual (F1)",
		"Tooltip text for the '?' button to open the manual."),
	preferences ("Preferences",
		"Window title for the preferences dialog and tooltip text for the button " +
		"to open the preferences dialog."),
	indexing ("Indexing...",
		"Message shown in the status bar to indicate that an indexing " +
		"process is running in the background."),
	web_interface ("Web Interface",
		"Window title for the web interface dialog and the tooltip text for " +
		"the button to open the web interface dialog."),
	to_systray (
		"Minimize To System Tray",
		"Label for a button to minimize the program into the system tray."),
	press_f1_for_help (
		"Press F1 for help.",
		"On startup, this message shown in the status bar to indicate that " +
		"the user can press F1 to open the manual."),
	invalid_query (
		"Invalid query. Reason:",
		"This message is shown after the user has entered an invalid query " +
		"into the search field. More detailed information is shown below " +
		"this message."),
		
	// Web interface
	enable_web_interface (
		"&Enable Web Interface",
		"Label for a checkbox button to enable or disable the web interface."),
	
	// System tray
	systray_not_available (
		"The system tray is not available.",
		"This error message is shown if the program could not be minimized " +
		"into the system tray."),
	restore_app ("Restore",
		"Entry in the system tray context menu for restoring the program after it has " +
		"been hidden in the system tray."),
	exit ("Exit",
		"Entry in the system tray context menu for terminating the program after it " +
		"has been hidden in the system tray."),
	
	// Indexing dialog
	select_folder_title (
		"Select Folder",
		"Window title for a folder chooser dialog."),
	select_folder_msg (
		"Please select the folder to be indexed.",
		"Message on a folder chooser dialog that prompts the user to select " +
		"a folder for indexing."),
	select_archive_title (
		"Select Archive File",
		"Window title for a file chooser dialog where the user is expected " +
		"to select an archive file."),
	select_outlook_pst_title (
		"Select Outlook PST File",
		"Window title for a file chooser dialog where the user is expected " +
		"to select an Outlook PST file."),
	found_pst_file (
		"PST file found:\n{0}\n\nNavigate to this file?",
		"This message is displayed after the user has chosen to create an " +
		"Outlook PST index and the program has found a PST file in the " +
		"standard location. The path of the PST file will be inserted into " +
		"the {0} slot. The message asks whether it should automatically " +
		"navigate to the PST file when opening the file chooser dialog."),
	help ("&Help",
		"Label for a button on a configuration window. Clicking the button opens a " +
		"manual page where the various configuration options are described."),
	restore_default (
		"&Restore Default",
		"Label for a button on a configuration window. Clicking the button " +
		"restores the default configuration (one setting)."),
	restore_defaults (
		"&Restore Defaults",
		"Label for a button on a configuration window. Clicking the button " +
		"restores the default configuration (multiple settings)."),
	run ("&Run",
		"Label for a button on the indexing configuration window. Clicking it starts the " +
		"indexing of the selected folder."),
	select_exts ("Please select one or more file extensions:",
		"The message on a dialog where the user can choose one or more file " +
		"extensions from a list."),
	overlaps_not_allowed (
		"Overlaps between indexes are not allowed.",
		"This error message is shown when the user attempts to add an index " +
		"to the indexing queue that would overlap with existing indexes. " +
		"For example, if one index represented the folder 'C:\\mydocs\\folder' " +
		"and another index represented the folder C:\\mydocs, the two indexes " +
		"would overlap."),
	
	// Indexing dialog: Queue
	indexing_queue (
		"Indexing Queue",
		"Window title of the indexing dialog (i.e. the dialog on which the " +
		"user can create or update indexes)."),
	add_to_queue (
		"Add To Queue",
		"Tooltip text of a button that opens a menu. The latter contains " +
		"various entries for adding indexes to the indexing queue."),
	add_folder (
		"Add Folder...",
		"Menu entry for adding folder indexes to the indexing queue."),
	add_archive (
		"Add Archive...",
		"Menu entry for adding archive indexes to the indexing queue."),
	add_outlook_pst (
		"Add Outlook PST...",
		"Menu entry for adding Outlook PST indexes to the indexing queue."),
	add_from_clipboard (
		"Add From Clipboard...",
		"Menu entry for adding indexes to the indexing queue, based on the " +
		"contents of the system clipboard."),
	minimize_to_status_bar (
		"Minimize To Status Bar",
		"Label of a button for minimizing the indexing dialog to the " +
		"program's status bar."),
	abort_indexing (
		"Abort Indexing?",
		"Window title of a confirmation dialog. This dialog is shown when " +
		"the user tries to close the indexing dialog or to terminate the " +
		"program while an indexing process is running."),
	keep_partial_index (
		"You are about to abort an indexing process. Do you want to keep the " +
		"index created so far? Keeping it allows you to continue indexing " +
		"later by running an index update.",
		//
		"This confirmation message is shown when the user tries to close the " +
		"indexing dialog while an indexing process is running."),
	keep_partial_index_on_exit (
		"An indexing process is still running and must be cancelled before " +
		"terminating the program. Do you want to keep the index created so far? " +
		"Keeping it allows you to continue indexing later by running an " +
		"index update.",
		//
		"This confirmation message is shown when the user tries to terminate " +
		"the program while an indexing process is running."),
	keep ("&Keep",
		"Label of a button on the dialog to cancel an indexing process. " +
		"If the user clicks on this button, the program will keep the " +
		"partially created index."),
	discard ("&Discard",
		"Label of a button on the dialog to cancel an indexing process. " +
		"If the user clicks on this button, the program will discard the " +
		"partially created index."),
	dont_abort ("Don't &Abort",
		"Label of a button on the dialog to cancel an indexing process. " +
		"If the user clicks on this button, the dialog is closed without " +
		"cancellation of the indexing process."),
	dont_exit ("Don't &Exit",
		"Label of a button on the dialog to cancel an indexing process. " +
		"If the user clicks on this button, the dialog is closed without " +
		"cancellation of the indexing process and without termination of " +
		"the program."),
	
	// Indexing dialog: Options
	file_extensions (
		"File extensions",
		"Label for a group of controls. The latter allow the user to specify " +
		"custom file extensions."),
	plain_text (
		"Plain text:",
		"Label for a text field into which the user can enter file extensions " +
		"for plain text files."),
	zip_archives (
		"Zip archives:",
		"Label for a text field into which the user can enter file extensions " +
		"for zip archives."),
	listing_ext_inside_archives (
		"Sorry, listing file extensions inside archives is not supported.",
		"Error message that is shown when the clicks on a button to collect " +
		"file extensions from the filesystem and display them as a list. " +
		"This feature is currently only supported for folders, but not for " +
		"archives."),
	exclude_files_detect_mime_type (
		"Exclude files / detect mime type",
		"Group label for controls that allow the user to specify (1) which files " +
		"to exclude from indexing and (2) for which files to activate mime " +
		"type detection."),
	miscellaneous (
		"Miscellaneous",
		"Group label for various indexing options."),
	indexing_options (
		"Indexing options",
		"Group label for various indexing options (applies to Oulook PST " +
		"indexes, where this is the only group of controls)."),
	index_html_pairs (
		"Index HTML pairs as single documents",
		"Label of a checkbox button to enable/disable HTML pairing. "
		+ Comments.seeIndexingOptions),
	detect_exec_archives (
		"Detect executable zip and 7z archives (slower)",
		"Label of a checkbox button to enable/disable detection of executable " +
		"zip and 7z archives. " + Comments.seeIndexingOptions),
	index_filenames (
		"Index filename even if file contents can't be extracted",
		"Label of a checkbox button to enable/disable filename indexing. " +
		Comments.seeIndexingOptions),
	store_relative_paths (
		"Store relative paths if possible (for portability)",
		"Label of a checkbox button to enable/disable storage of relative " +
		"paths. " + Comments.seeIndexingOptions),
	watch_folders (
		"Watch folders for file changes",
		"Label of a checkbox button to enable/disable folder watching. " +
		Comments.seeIndexingOptions),
	changing_store_relative_paths_setting (
		"Changing the 'store relative paths' setting might require adapting " +
		"some of the regular expressions in the pattern table that are " +
		"matched against paths.",
		//
		"The exclusion of files from indexing is based on matching regular " +
		"expressions against filenames or filepaths. If the user changes the " +
		"'store relative paths' setting, this will affect whether the " +
		"regular expressions are matched against relative or against absolute " +
		"paths. This message serves as a reminder that the user might have " +
		"to readjust his/her regular expressions after changing the 'store " +
		"relative paths' setting."),
	malformed_regex (
		"Malformed regular expression: {0}",
		"This message is shown when an invalid regular expression is found " +
		"on the file exclusion table. The invalid regular expression will be " +
		"inserted into the {0} slot."),
	confirm_text_ext (
		"You've entered the following plain text extensions: {0}. " +
		"This will override DocFetcher's built-in support for files with " +
		"these extensions, and the files will instead be treated as simple " +
		"text files.\n\nThis is probably not what you want because the " +
		"built-in support will generally give better text extraction results. " +
		"Do you still want to continue?",
		//
		"This warning message is shown when the user has entered custom plain text " +
		"extension that would override the built-in support for certain document formats " +
		"such as *.doc or *.odt. The user's plain text extensions will be " +
		"inserted into the {0} slot."),
	confirm_zip_ext (
		"You've entered the following zip extensions: {0}. This will override " +
		"DocFetcher's built-in support for files with these extensions, and " +
		"the files will instead be treated as zip archives. Do you still want " +
		"to continue?",
		//
		"This warning message is shown when the user has entered custom zip " +
		"extensions that would override the built-in support for certain " +
		"document formats such as *.doc or *.odt. The user's zip extensions " +
		"will be inserted into the {0} slot."),
	
	// Indexing dialog: Pattern table
	pattern_regex ("Pattern (regex)"),
	match_against ("Match Against"),
	relative_path ("Relative path"),
	absolute_path ("Absolute path"),
	action ("Action"),
	exclude ("Exclude"),
	detect_mime_type ("Detect mime type (slower)"),
	add_pattern ("Add Pattern"),
	remove_sel_pattern ("Remove Selected Pattern"),
	increase_pattern_priority ("Increase Priority Of Selected Pattern"),
	decrease_pattern_priority ("Decrease Priority Of Selected Pattern"),
	sel_regex_matches_file_yes ("Selected regex matches following file: Yes."),
	sel_regex_matches_file_no ("Selected regex matches following file: No."),
	sel_regex_malformed ("Selected regex is malformed."),
	choose_regex_testfile_title ("Select File"),
	
	// Indexing dialog: Progress
	progress ("Progress"),
	errors ("Errors"),
	error ("Error: {0}"),
	file_corrupted ("Unknown file format."),
	doc_pw_protected ("Document is password protected."),
	out_of_memory ("Out Of Memory"),
	out_of_memory_instructions ("DocFetcher has run out of memory. " +
		"Please see the relevant <a href=\"{0}\">manual page</a> for further instructions."), // {0} slot
	out_of_memory_instructions_brief ("Out of memory. See the manual for instructions on how to raise the memory limit."),
	not_enough_diskspace ("Not enough diskspace on '{0}' to unpack archive entries. Available: {1} MB. Needed: {2} MB."),
	archive_encrypted ("Archive is encrypted."),
	archive_entry_encrypted ("Archive entry is encrypted."),
	not_an_archive ("Not an archive."),
	parser_not_found ("Could not find a suitable parser."),
	document ("Document"),
	error_message ("Error Message"),
	total_elapsed_time ("Total elapsed time: {0}"),
	copy ("Copy\tCtrl+C"),
	install_watch_failed (
	"Failed to install watch on folder '{0}'.\n\nInternal error message:\n{1}"),
	
	// Preview panel
	loading ("Loading...",
		"A generic loading message that is used at various places, e.g. when " +
		"loading a file for display in the preview pane."),
	occurrence_count (
		"Number Of Occurrences",
		"Number of textual matches (usually whole words) that are highlighted " +
		"in the preview pane."),
	prev_occurrence (
		"Previous Occurrence",
		"Label of a button for jumping to the previous match in the preview pane."),
	next_occurrence (
		"Next Occurrence",
		"Label of a button for jumping to the next match in the preview pane."),
	highlighting_on_off (
		"Highlighting On/Off",
		"Label of a button for turning the match highlighting in the preview " +
		"pane on and off."),
	prev_page ("Previous Page",
		"Built-in web browser: Go one page backwards in the history of " +
		"visited pages."),
	next_page ("Next Page",
		"Built-in web browser: Go one page forwards in the history of " +
		"visited pages."),
	browser_stop (
		"Stop Loading Current Page",
		"Built-in web-browser: Stop loading the currently loaded page."),
	browser_refresh (
		"Refresh Current Page",
		"Built-in web-browser: Refresh the currently displayed page."),
	browser_launch_external (
		"Open In External Browser",
		"Built-in web-browser: Open the currently displayed page in an " +
		"external web browser (e.g. Firefox)."),
	use_embedded_html_viewer (
		"Use Embedded HTML Viewer (If Available)",
		"Preview pane: Switch from the text-only preview to the web browser " +
		"based HTML preview."),
	email ("Email"),
	from_field ("From:"),
	to_field ("To:"),
	subject_field ("Subject:"),
	date_field ("Date:"),
	
	// Preferences
	pref_manual_on_startup (
		"Show manual on startup",
		Comments.prefOption + "Whether to show the manual when the program starts."),
	pref_use_or_operator (
		"Use OR operator as default in queries (instead of AND)",
		Comments.prefOption + "Whether to concatenate two or more consecutive words in a " +
		"query with OR instead of AND. Example: When the user submits the " +
		"query 'dog cat', this would be treated as 'dog OR cat'."),
	pref_hide_in_systray (
		"Hide program in System Tray after opening files",
		Comments.prefOption + "Whether the program should be minimized to the " +
		"system tray after the user opens a file in the result pane."),
	pref_clear_search_history_on_exit (
		"Clear search history on exit",
		Comments.prefOption + "Whether the search history (i.e. the list of " +
		"recently entered queries) should be cleared when the program exits."),
	pref_highlight_color (
		"Highlight color:",
		Comments.prefOption + "The color to use for highlighting matches in " +
		"the preview pane."),
	pref_font_normal (
		"Preview font (normal):",
		Comments.prefOption + "The font to use in the preview pane; applies to " +
		"all file formats except plain text."),
	pref_font_fixed_width (
		"Preview font (fixed width):",
		Comments.prefOption + "The fixed-width font to use in the " +
		"preview pane; applies only to plain text files."),
	pref_hotkey (
		"Global hotkey:",
		Comments.prefOption + "The global hotkey to move the program window " +
		"to the top."),
	keybox_title (
		"Enter Key",
		"Window title for a dialog where the user can change a keyboard shortcut."),
	keybox_msg (
		"Please enter a key:",
		"The message displayed on the 'Enter Key' dialog."),
	
	// Hotkeys
	f1 ("F1"),
	f2 ("F2"),
	f3 ("F3"),
	f4 ("F4"),
	f5 ("F5"),
	f6 ("F6"),
	f7 ("F7"),
	f8 ("F8"),
	f9 ("F9"),
	f10 ("F10"),
	f11 ("F11"),
	f12 ("F12"),
	pause_key ("Pause"),
	print_screen_key ("Print Screen"),
	backspace_key ("Backspace"),
	enter_key ("Enter"),
	insert_key ("Insert"),
	delete_key ("Delete"),
	home_key ("Home"),
	end_key ("End"),
	page_up_key ("Page Up"),
	page_down_key ("Page Down"),
	arrow_up ("Arrow Up"),
	arrow_down ("Arrow Down"),
	arrow_left ("Arrow Left"),
	arrow_right ("Arrow Right"),
	alt_key ("Alt"),
	shift_key ("Shift"),
	ctrl_key ("Ctrl"),
	command_key ("Command"),
		
	// Result panel and status bar
	num_results ("Results: {0}"),
	num_sel_results ("Selected: {0}"),
	title ("Title"),
	score ("Score [%]"),
	size ("Size"),
	filename ("Filename"),
	type ("Type"),
	path ("Path"),
	authors ("Authors"),
	last_modified ("Last Modified"),
	subject ("Subject"),
	sender ("Sender"),
	send_date ("Send Date"),
	open ("Open"),
	open_parent ("Open Parent Folder"),
	open_limit ("The number of entries that can be opened simultaneously is limited to {0}."),
	
	/*
	 * TODO post-release-1.1: Various message strings that require implementation of features
	 */
	invert_selection ("Invert Selection"),
	no_files_in_cb ("No files in clipboard found."),
	preview_limit_hint ("DocFetcher: The number of characters exceeded the limit of {0} characters. You can increase the limit by modifying the \"{1}\" entry in the {2} file; however, this might crash the program."),
	jobs ("{0} Job(s)"),
	open_file_error ("Error on opening file: {0}"),
	enter_nonempty_string ("Please enter a non-empty search string."),
	search_scope_empty ("Cannot perform search: No folders have been indexed yet."),
	minsize_not_greater_maxsize ("The minimum filesize must not be greater than the maximum filesize."),
	filesize_out_of_range ("Filesizes must be between 0 and (2^63 - 1) Bytes."),
	no_filetypes_selected ("No filetypes have been selected."),
		
	num_results_detail ("Results: {0}-{1} of {2}"),
	num_documents_added ("Documents added: {0}"),
	;
	
	private static boolean checkEnabled = true;
	private final String value;
	private final String comment;
	
	Msg(@NotNull String defaultValue) {
		this(defaultValue, "");
	}
	
	Msg(@NotNull String defaultValue, @NotNull String comment) {
		Util.checkNotNull(defaultValue, comment);
		this.value = defaultValue;
		this.comment = comment;
	}
	
	@NotNull
	public String get() {
		assert !checkEnabled || !value.contains("{0}");
		return value;
	}
	
	@NotNull
	public String getComment() {
		return comment;
	}
	
	/**
	 * Returns a string created from a <tt>java.text.MessageFormat</tt>
	 * with the given argument(s).
	 */
	public String format(Object... args) {
		return MessageFormat.format(value, args);
	}
	
	public static void setCheckEnabled(boolean checkEnabled) {
		Msg.checkEnabled = checkEnabled;
	}
	
	private static final class Comments {
		private static final String filetype
			= "An entry in the 'document types' filter control.";
		private static final String searchScopeEntry
			= "An entry in the context menu of the 'Search Scope' control.";
		private static final String createIndexFromEntry
			= "Submenu entry in the 'Create Index From' menu.";
		private static final String prefOption
			= "Option on the preferences window: ";
		private static final String seeIndexingOptions
			= "See the manual section 'Indexing Options' for an explanation.";
	}

}
