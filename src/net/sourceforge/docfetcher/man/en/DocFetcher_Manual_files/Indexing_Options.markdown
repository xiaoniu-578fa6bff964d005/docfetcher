Basic Idea: A Queue of Indexing Tasks
=====================================
The indexing configuration window contains one or more tabs, each of which represents an index to be created or updated. All the tabs together form a task queue, whose items are processed one by one. You can add more tasks to the queue via the `'+'` button on the top right.

Each tab has a `Run` button on the bottom right. By clicking it, you confirm that the task is properly configured and ready for indexing. The indexing starts as soon as there is one ready task in the queue.

On the right of the `'+'` button is another button. Clicking this one will minimize the whole configuration window into DocFetcher's status bar. This allows you to perform searches on the existing indexes while creating new ones in the background.

You can cancel any task by clicking on the close button (`'x'`) of its tab. When a task is cancelled, you'll be given the choice of either keeping or discarding the partially created index. The point is that you can stop indexing at any point and resume later. Resuming is simply done by running an index update on the partial index, via `Update Index` from the `Search Scope` area's context menu.

The configuration window itself has a close button as well (on Windows, this is the `'x'` button on the top right). If you click on it, the whole queue will be cleared, and all indexing tasks will be cancelled.

Indexing Options
================
Note: This section focuses on the available indexing options for folder and archive indexes. For the options for Outlook PST indexes, see the relevant entries in the 'Miscellaneous' table below.

File Extensions
---------------
The 'file extensions' control allows you to specify which files should be treated as plain text files or zip archives. One common usage scenario is to make DocFetcher index certain kinds of source code files. Notice the two `'...'` buttons on the right? If you click on these, DocFetcher will walk through the folder to be indexed and gather all file extensions into a list for you to choose from.

Exclude Files / Detect Mime Type
--------------------------------
By adding items to the table, you can (1) exclude certain files from indexing, and (2) enable mime type detection for certain files. This is all based on regular expressions (regexes), so if you don't know how to use them, read up on the [introduction to regular expressions](Regular_Expressions.html).

Now, here's how the table works: Each item in the table is a regex with an associated action. The regex can be matched either against filenames or against filepaths, and the action is either "exclude file" or "detect mime type". During indexing, when a file is matched by a regex, the action of the regex is applied to the file.

You can add items to and remove items from the table with the `'+'` and `'-'` buttons on the right. The up and down buttons allow you increase or decrease the *priority* of the selected table item. The priority becomes significant when a file is matched by more than one regex in the table; in that case the regex with the highest priority wins, and all others are ignored.

Directly below the table, there's a little tool that helps you with writing regexes: Click on the `'...'` button on the right of it to choose a certain file from the folder to be indexed. The filepath of this file will show up in the text field. Then the text line above the text field will tell you whether the regex currently selected in the table matches the selected file.

Miscellaneous
-------------
Option | Comment
-------|--------
HTML pairing  |  Whether HTML files and their associated folders (e.g. a file `foo.html` and a folder `foo_files`) should be treated as a single document.
Detect executable zip and 7z archives (slower)  |  If enabled, DocFetcher will check for *every* file with the extension `exe` whether it's an executable zip or 7z archive.
Index filename even if file contents can't be extracted  |  If enabled, DocFetcher will include *all* files in its index, regardless of whether any file contents can be extracted. Enable this for full filename searching. Note however that DocFetcher might then take up a lot more memory, depending on the number of files in the indexed folder. In case you run out of memory, you can [raise the memory limit](Memory_Limit.html).
Store relative paths if possible (for portability)  |  This setting is important if you're using the portable version of DocFetcher. You can read more about this on the page about [portable document repositories](Portable_Repositories.html).
Watch folders for file changes  |  Whether DocFetcher should detect changes in the indexed folders and update its indexes accordingly. This setting does not affect the DocFetcher daemon.
