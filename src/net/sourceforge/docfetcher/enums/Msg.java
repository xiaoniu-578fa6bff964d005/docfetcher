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

import net.sourceforge.docfetcher.util.annotations.NotNull;

public enum Msg {
	
	// TODO pre-release: remove unused message strings
	
	search_with_docfetcher ("Search With DocFetcher"),
	report_bug ("Ooops! This program just died! " +
			"Please help us to fix this problem by " +
			"posting the stacktrace below on our " +
			"<a href=\"http://sourceforge.net/apps/mediawiki/docfetcher/index.php?title=Bug_Reports\">bug tracker</a> (no registration required).\n\n" +
			"The stacktrace has been written to:\n{0}."),
	read_error ("Cannot access file: {0}"),
	write_error ("Cannot write to disk!"),
	write_warning ("Disk is not writable. Changes to the preferences and indexes will not be saved."),
	hotkey_in_use ("The hotkey {0} DocFetcher tried to register seems to be already in use. Please set an unused hotkey in the preferences."),
	invalid_start_params ("Warning: DocFetcher was launched with invalid parameters."),
	yes ("Yes"),
	no ("No"),
	force_quit ("An indexing process is still running. Exit anyway?"),
	close ("Close"),
	
	filesize_group_label ("Minimum / Maximum Filesize"),
	filetype_group_label ("File Types"),
	invert_selection ("Invert Selection"),
	filetype_abi ("AbiWord (abw, abw.gz, zabw)"),
	filetype_chm ("MS Compiled HTML Help (chm)"),
	filetype_doc ("MS Word (doc)"),
	filetype_docx ("MS Word 2007 (docx, docm)"),
	filetype_ppt ("MS Powerpoint (ppt)"),
	filetype_pptx ("MS Powerpoint 2007 (pptx, pptm)"),
	filetype_xls ("MS Excel (xls)"),
	filetype_xlsx ("MS Excel 2007 (xlsx, xlsm)"),
	filetype_vsd ("MS Visio (vsd)"),
	filetype_html ("HTML (html, htm, ..)"),
	filetype_odt ("OpenOffice.org Writer (odt, ott)"),
	filetype_ods ("OpenOffice.org Calc (ods, ots)"),
	filetype_odg ("OpenOffice.org Draw (odg, otg)"),
	filetype_odp ("OpenOffice.org Impress (odp, otp)"),
	filetype_pdf ("PDF Document (pdf)"),
	filetype_rtf ("Rich Text Format (rtf)"),
	filetype_svg ("Scalable Vector Graphics (svg)"),
	filetype_txt ("Plain Text"),
	filetype_wpd ("WordPerfect (wpd)"),
	
	search_scope ("Search Scope"),
	create_index ("Create Index..."),
	contains_file ("The selection contains at least one file. Indexing aborted."),
	invalid_dnd_source ("Invalid Drag and Drop source."),
	update_index ("Update Index"),
	rebuild_index ("Rebuild Index..."),
	remove_index ("Remove Index"),
	remove_sel_indexes ("Remove selected indexes?"),
	remove_orphaned_indexes ("Remove Orphaned Indexes"),
	remove_orphaned_indexes_msg ("Remove all indexes whose document folders are missing?"),
	check_toplevel_only ("Check Top-Level Only"),
	uncheck_toplevel_only ("Uncheck Top-Level Only"),
	check_all ("Check All"),
	uncheck_all ("Uncheck All"),
	open_folder ("Open Folder"),
	list_docs ("List Documents"),
	folders_not_found_title ("Folder(s) not found"),
	folders_not_found ("The following folders do not exist anymore:"),
	create_subfolder ("Create Subfolder..."),
	enter_folder_name ("Enter Folder Name"),
	enter_folder_name_new ("Enter the name for the new folder:"),
	create_subfolder_failed ("Failed to create subfolder."),
	rename_folder ("Rename Folder..."),
	rename_requires_full_rebuild ("Renaming a root folder requires a full index rebuild. Do you want to continue?"),
	enter_new_foldername ("Enter a new folder name:"),
	cant_rename_folder ("Cannot rename folder."),
	delete_folder ("Delete Folder"),
	delete_folder_q ("Do you really want to remove the following folders?"),
	paste_into_folder ("Paste Files Into Folder"),
	toggle_delete_on_exit ("Toggle Deletion On Exit"),
	open_target_folder ("Open Target Folder"),
	file_already_exists ("File already exists: {0}"),
	file_already_exists_dot ("File already exists."),
	folder_already_exists ("Folder already exists."),
	folder_not_found ("Folder not found."),
	file_transfer ("File Transfer"),
	no_files_in_cb ("No files in clipboard found."),
	moving_files ("Moving {0} file(s) ({1} KB) to: {2}"),
	copying ("Copying: {0}"),
	deleting ("Deleting: {0}"),
	prev_page ("Previous page"),
	next_page ("Next page"),
	preferences ("Preferences"),
	to_systray ("Minimize to system tray"),
	occurrence_count ("Number of occurrences"),
	prev_occurrence ("Previous occurrence"),
	next_occurrence ("Next occurrence"),
	open_manual ("Open manual"),
	use_embedded_html_viewer ("Use embedded HTML viewer (if available)"),
	browser_stop ("Stop loading current page"),
	browser_refresh ("Refresh current page"),
	browser_launch_external ("Open in external browser"),
	loading ("Loading..."),
	cant_read_file ("Cannot read file: {0}"),
	preview_limit_hint ("DocFetcher: The number of characters exceeded the limit of {0} characters. You can increase the limit by modifying the \"{1}\" entry in the {2} file; however, this might crash the program."),
	systray_not_available ("The system tray is not available."),
	restore_app ("Restore"),
	exit ("Exit"),
	jobs ("{0} Job(s)"),
	open_file_error ("Error on opening file: {0}"),
	enter_nonempty_string ("Please enter a non-empty search string."),
	invalid_query ("Invalid query. Reason:"),
	invalid_query_syntax ("Invalid Query Syntax"),
	leading_wildcard ("Searches with leading wildcards (* or ?) are slower due to technical limitations."),
	search_scope_empty ("Cannot perform search: No folders have been indexed yet."),
	minsize_not_greater_maxsize ("The minimum filesize must not be greater than the maximum filesize."),
	filesize_out_of_range ("Filesizes must be between 0 and (2^63 - 1) Bytes."),
	no_filetypes_selected ("No filetypes have been selected."),
	press_help_button ("Press {0} for help."),
	num_results ("Results: {0}"),
	num_results_detail ("Results: {0}-{1} of {2}"),
	page_m_n ("Page {0}/{1}"),
	num_sel_results ("Selected: {0}"),
	num_documents_added ("Documents added: {0}"),
	pref_manual_on_startup ("Show manual on startup"),
	pref_close_tabs ("Close tabs after successful indexing"),
	pref_watch_fs ("Watch indexed folders"),
	pref_use_or_operator ("Use OR operator as default in queries (instead of AND)"),
	pref_hide_in_systray ("Hide program in System Tray after opening files"),
	pref_highlight ("Highlight search terms in the preview panel"),
	pref_clear_search_history_on_exit ("Clear search history on exit"),
	pref_highlight_color ("Highlight color:"),
	pref_text_ext ("Text file extensions (default):"),
	pref_html_ext ("HTML file extensions (default):"),
	pref_skip_regex ("Skip files (default):"),
	pref_max_results ("Results per page:"),
	pref_max_results_range ("The maximum number of results per page must be between 1 and {0}."),
	keybox_title ("Enter Key"),
	keybox_msg ("Please enter a key:"),
	pref_hotkey ("Global hotkey:"),
	restore_defaults ("Restore Defaults"),
	help ("Help"),
	scope_folder_title ("Select Folder"),
	scope_folder_msg ("Please choose the folder to add to the search scope."),
	index_management ("Index Management"),
	target_folder ("Target folder:"),
	ipref_text_ext ("Text extensions:"),
	ipref_html_ext ("HTML extensions:"),
	select_exts ("Please select one or more file extensions:"),
	ipref_skip_regex ("Skip files (regex):"),
	ipref_detect_html_pairs ("Combine HTML files and their associated folders"),
	ipref_delete_on_exit ("Delete index on exit"),
	run ("Run"),
	regex_matches_file_yes ("Regex pattern above matches following file: Yes."),
	regex_matches_file_no ("Regex pattern above matches following file: No."),
	target_folder_deleted ("Target folder has been deleted!"),
	not_a_regex ("Not a regular expression: {0}"),
	add_to_queue ("Add folder to queue"),
	inters_indexes ("Intersections between indexes are not allowed."),
	inters_queue ("Intersection with queue entry detected."),
	choose_regex_testfile_title ("Select file"),
	discard_incomplete_index ("Incomplete index will be discarded. Do you want to continue?"),
	progress ("Progress"),
	html_pairing ("HTML pairing:"),
	waiting_in_queue ("Waiting in queue..."),
	file_skipped ("Skipped: {0}"),
	finished ("Finished."),
	finished_with_errors ("Finished with errors."),
	total_elapsed_time ("Total elapsed time: {0}"),
	errors ("Errors"),
	error_type ("Error Type"),
	out_of_jvm_memory ("Not enough memory left in the Java Virtual Machine. For more information, please refer to the manual."),
	file_not_found ("File not found."),
	file_not_readable ("Unable to read file."),
	file_corrupted ("Unknown file format."),
	unsupported_encoding ("Unsupported Encoding."),
	doc_pw_protected ("Document is password protected."),
	no_extraction_permission ("No permission to extract text."),
	parser_error ("Parser error."),
	wordperfect_expected ("Unknown file format; WordPerfect document expected."),
	wordperfect_parser_not_installed ("WordPerfect parser not installed. Please visit: http://docfetcher.sourceforge.net/download.html"),
	send_file_for_debugging ("Parser crash. Please send this file to docfetcher@users.sourceforge.net for debugging."),
	open ("Open"),
	open_parent ("Open Parent Folder"),
	open_limit ("The number of entries that can be opened simultaneously is limited to {0}."),
	copy ("Copy"),
	delete_file ("Delete File"),
	confirm_delete_file ("{0} document(s) will be deleted. Do you want to continue?"),
	empty_folders ("Empty Folders"),
	empty_folders_msg ("The following folders are empty now.\nYou can open a folder by doubleclicking on it."),
	property_title ("Title"),
	property_score ("Score [%]"),
	property_size ("Size"),
	property_name ("Filename"),
	property_type ("Type"),
	property_path ("Path"),
	property_author ("Author"),
	property_lastModified ("Last Modified"),
	parser_testbox ("Parser Testbox"),
	choose_file ("Choose File..."),
	enter_path_msg ("Enter a path here and press Enter or click on the button to the right."),
	original_parser_output ("Original parser output"),
	parser_testbox_info ("Please note:\n* HTML pairing is disabled.\n* The parse time shown here will decrease significantly if you parse the same document several times over."),
	parser_testbox_invalid_input ("File does not exist, or path string format is unsupported or malformed."),
	unknown_document_format ("Unknown document format."),
	parsing ("Parsing..."),
	parser_not_supported ("Parser not supported: {0}"),
	parsed_by ("Parsed by {0} in {1} ms\nTitle: \"{2}\"\nAuthor: \"{3}\""),
	parse_exception ("Parse Exception: {0}"),
	parser_testbox_unknown_error ("Something went really bad..."),
	search ("Search"),
	first_page ("First page"),
	last_page ("Last page"),
	current_result_page ("{0} results found / displaying {1} - {2}"),
	
	;
	
	private String value;
	
	Msg(String defaultValue) {
		this.value = defaultValue;
	}
	
	@NotNull
	public String get() {
		return value;
	}
	
	/**
	 * Returns a string created from a <tt>java.text.MessageFormat</tt>
	 * with the given argument(s).
	 */
	public String format(Object... args) {
		return MessageFormat.format(value, args);
	}

}
