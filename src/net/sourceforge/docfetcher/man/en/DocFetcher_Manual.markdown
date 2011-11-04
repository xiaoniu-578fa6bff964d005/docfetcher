Introduction
============

DocFetcher is an Open Source desktop search application: It allows you to search the contents of documents on your computer. - You can think of it as Google for your local document repository.

**Index-based search**: Since searching in the documents directly would be impractically slow for a larger number of documents, DocFetcher requires that you create *indexes* for the folders you want to search in. These indexes allow DocFetcher to quickly look up files by keyword, similar to how you would use the index in the back of a book. Creating an index might take some time, depending on the number and sizes of the indexed files. However, this needs to be done only once for each folder; afterwards you can search in the folders as many times as you want.

**Creating an index**: To create an index, right-click on the `Search Scope` area on the left and select `Create Index From > Folder`. Now choose a folder to be indexed. For starters, this should be a small one with not too many files in it, like, say, 50? After selecting the folder, a configuration window pops up. The default configuration should suffice, so just click on the `Run` button and wait until DocFetcher has finished indexing the documents. (An alternative way of creating an index is to paste a directory from the clipboard into the `Search Scope` area.)

**Searching**: Enter one or more words to search for in the text field above the result pane (the table with the column headers) and press the `Enter` key. The search results will be displayed in the result pane, sorted by descending score.

*If you are reading this manual inside DocFetcher, following the instructions in the next paragraph will make the manual disappear. To restore it, click on the `'?'` button on the top right. You can also open the manual in your default web browser by clicking on the `Open in external browser` button directly above this pane.*

**Result pane and preview pane**: Below the result pane (or to the right of it, depending on the current GUI layout), you can find the preview pane. If you select a file on the result pane, the preview pane will show a text-only preview of the file's contents. Notable features:

* ***Highlighting***: By default, the search terms you've entered will be highlighted, and you can jump from one occurrence to the next or previous using the up and down buttons.
* ***Built-in web browser***: In case of HTML files, you can switch between the text-only view and a simple built-in web browser. (Note: The latter is not available on some Linux variants.)

One shortcut you might find useful: Press `Ctrl+F` or `Alt+F` to move the focus back to the search field. To open a file in an external program, double-click on it in the result pane.

**Sorting**: You can change the sorting of the results by clicking on any of the result pane's column headers. For example, to sort the results by filename, click on the `Filename` header. Clicking the same header twice will sort by the corresponding criterion in reversed order. You can also change the order of the columns via drag and drop: For example, if you want `Filename` to be the first column, just drag the `Filename` column to the left.

**Filtering**: On the left of the GUI, you can see various controls for filtering the results: (1) You can specify a minimum and/or maximum filesize in the `Minimum / Maximum Filesize` control. (2) The `Document Types` list allows you to filter the results by type. (3) By unchecking items in the `Search Scope` area, you can filter the results by location.

**Index updates**: If files in the indexed folders are added, removed or modified, the corresponding indexes have to be updated, otherwise your search results might be out of date. Fortunately, updating an index is virtually always much faster than creating it from scratch, since only the changes have to be processed. Also, DocFetcher can update its indexes automatically in two ways:

1. ***DocFetcher itself***: If DocFetcher is running and the *folder watching* for the modified folder is enabled, DocFetcher detects the changes and updates its indexes immediately.
2. ***DocFetcher daemon***: If DocFetcher isn't running, the changes are recorded by a small daemon program that runs in the background; the affected indexes will then be updated the next time DocFetcher starts. (Note: The daemon is currently not available on Mac OS X, unfortunately.)

Some caveats: If you are using the portable version of DocFetcher and want the daemon to be run, you must install it manually by adding the daemon executable to your operating system's list of startup programs. Also, neither DocFetcher nor the daemon can detect changes on network shares.<!-- this line should end with two spaces -->  
So, in those cases where the indexes can't be updated automatically, you'll have to do it yourself: In the `Search Scope` area, select one or more indexes to be updated, then either click on `Update Index` in the `Search Scope` context menu, or press the `F5` key.

* * *

<a name="Advanced_Usage"></a> <!-- Do not translate this line, just copy it verbatim. -->
Advanced Usage
==============

**Query syntax**: With DocFetcher, you can do much more than simple word lookup. For example, you can use wildcards to search for words with a common start, like so: `wiki*`. To search for a certain phrase (i.e. a sequence of words in a specific order), surround the phrase with quotation marks: `"the quick brown fox"`. But that's barely the start. For an overview of all the supported contructs, see the [query syntax section](DocFetcher_Manual_files/Query_Syntax.html).

**Portable document repository**: The portable version of DocFetcher allows you create a bundle containing DocFetcher, your documents and the associated indexes. You can then freely move this bundle around, e.g. onto CD-ROMs, USB drives, encrypted volumes or other computers. This is also useful for sharing a single document repository across a local area network or across different operating systems. One important thing to keep in mind here is that the indexes must be created with *relative paths*. [Details](DocFetcher_Manual_files/Portable_Repositories.html).

**Indexing configuration options**: For a detailed discussion of all those options on the indexing configuration window, click [here](http://todo.com). Perhaps the most interesting ones are:

* customizable file extensions for plain text files and zip archives
* exclusion of certain files from indexing based on regular expressions
* mime type detection
* detection of HTML pairs

* * *

Caveats and Common Gotchas
==========================

**Raising the memory limit**: DocFetcher, like all Java programs, has a fixed limit on how much memory it's allowed to use, known as the *Java heap size*. This memory limit must be set on startup, and DocFetcher currently chooses a default value of 256 MB. If you try to index a very, very large number of files, and/or if some of the indexed files are really huge (which is not uncommon with PDF files), then chances are DocFetcher will hit that memory limit. If this ever happens, you might want to [raise the memory limit](http://todo.com).

**Don't index system folders**: In contrast to other desktop search applications, DocFetcher was not designed for indexing system folders such as `C:`or `C:\Windows`. Doing so is discouraged for the following reasons:

1. ***Slowdown***: The files in system folders tend to be modified very frequently. If the folder watching is turned on, this will cause DocFetcher to update its indexes all the time, slowing down your computer.
2. ***Memory issues***: DocFetcher needs to keep tiny representations of your files in memory. Because of this, and because system folders usually contain a very large number of files, DocFetcher will be more likely to run out of memory if you index system folders.
3. ***Waste of resources, worse search results***: Apart from these technical reasons, indexing system folders is most likely a waste of indexing time and disk space, and it will also pollute your search results with unneeded system files. So, for the best results in the least amount of time, just index what you need.

**Unicode support**: DocFetcher has full Unicode support for all document formats except CHM files. In case of plain text files, DocFetcher has to use [certain heuristics](http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html) to guess the correct encoding, since plain text files don't contain any explicit encoding information.

**Archive support**: DocFetcher currently supports the following archive formats: zip and derived formats, 7z, rar and the whole tar.* family. Additionally, executable zip and 7z archives are supported as well (but not executable rar archives). DocFetcher will treat all archives as if they were ordinary folders, and it can also handle an arbitrarily deep nesting of archives (e.g. a zip archive containing a 7z archive containing a rar archive...).<!-- this line should end with two spaces -->  
With that said, it should be noted that support for zip and 7z archives is best in terms of robustness and speed. On the other hand, indexing of tar.gz, tar.bz2 and similar formats tends to be less efficient. This is due to the fact that these formats don't have an internal "summary" of the archive contents, which forces DocFetcher to unpack the entire archive rather than only individual archive entries. Bottom line: If you have the choice, compress your files either as zip or 7z archives for maximum compatibility with DocFetcher.

**Folder watch limit (Linux-only)**: On Linux, processes can by default watch at most 8192 folders. You might reach this limit if you index a very deep hierarchy of folders. If that happens, DocFetcher will probably print a warning message like "No space left on device" on the console. You can work around this by raising the watch limit. For example, this command will raise the watch limit to 32000:

    sudo echo 32000 > /proc/sys/fs/inotify/max_user_watches

**The DocFetcher daemon is innocent**: If you suspect that the DocFetcher daemon is slowing down your computer or causing crashes, you're probably wrong. As a matter of fact, the daemon is a very simple program with low memory footprint and CPU usage, and it doesn't do much besides watching folders. If you're still not convinced, just rename the daemon executables so they won't start automatically, or try the portable version of DocFetcher, where the daemon is deactivated by default.

* * *

Further information
===================

**The DocFetcher wiki**: For more information, have a look at our [wiki](http://sourceforge.net/apps/mediawiki/docfetcher/index.php?title=Main_Page), and the [Advanced Usage](http://sourceforge.net/apps/mediawiki/docfetcher/index.php?title=Advanced_Usage) section in particular.
