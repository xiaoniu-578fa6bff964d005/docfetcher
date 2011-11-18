Description
===========
DocFetcher is an Open Source desktop search application: It allows you to search the contents of documents on your computer. &mdash; You can think of it as Google for your local document repository. The application runs on Windows, Linux and Mac OS X, and is made available under the [Eclipse Public License](http://en.wikipedia.org/wiki/Eclipse_Public_License).

Basic Usage
===========
The screenshot below shows the main user interface. Queries are entered in the text field at (1). The search results are displayed in the result pane at (2). The preview pane at (3) shows a text-only preview of the file currently selected in the result pane. Any matches in the file are by default highlighted with a yellow background.

You can filter the results by minimum and/or maximum filesize (4), by file type (5) and by location (6). The buttons at (7) are used for opening the manual, opening the preferences and minimizing the program into the system tray, respectively.

<div id="img" style="text-align: center;"><a href="../all/intro-001-results-edited.png"><img style="width: 500px; height: 375px;" src="../all/intro-001-results-edited.png"></a></div>

In order to speed up its searches, DocFetcher requires creating so-called *indexes*. This will be explained in more detail below. You can create new indexes via the context menu of the location filter (6). This will open up a dialog with various indexing options, as shown in the following screenshot:

<div id="img" style="text-align: center;"><a href="../all/intro-002-config.png"><img style="width: 500px; height: 375px;" src="../all/intro-002-config.png"></a></div>

When you're done with customizing the indexing options, you can click on the "Run" button at the bottom right to start indexing. The indexing process can take a while, depending on the number and sizes of the files to index. A good rule of thumb is 200 files per minute.

Notable Features
================
* **A portable version**: There is a portable version of DocFetcher that runs on Windows, Linux *and* Mac OS X. More on that below.
* **64-bit support**: Both 32-bit and 64-bit operating systems are supported.
* **Unicode support**: DocFetcher comes with rock-solid Unicode support for all major formats, including Microsoft Office, OpenOffice.org, PDF, HTML, RTF and plain text files. The only exception is CHM, for which we don't have Unicode support yet.
* **Archive support**: DocFetcher supports the following archive formats: zip, 7z, rar, and the whole tar.* family. The file extensions for zip archives can be customized, allowing you to add more zip-based archive formats as needed. Also, DocFetcher can handle an unlimited nesting of archives (e.g. a zip archive containing a 7z archive containing a rar archive, containing... and so on).
* **Search in source code files**: The file extensions by which DocFetcher recognizes plain text files can be customized, so you can use DocFetcher for searching in any kind of source code. (This works quite well in combination with the customizable zip extensions, e.g. for searching in Java source code inside Jar files.)
* **Outlook PST files**: DocFetcher allows searching for Outlook emails, which are typically stored in PST files.
* **Detection of HTML pairs**: By default, DocFetcher detects pairs of HTML files (e.g. a file named "foo.htm" and a folder named "foo_files"), and treats the pair as a single document. This feature may seem rather useless at first, but it turned out that this dramatically increases the quality of the search results when you're dealing with HTML files, since all the "clutter" inside the HTML folders disappears from the results.
* **Regex-based exclusion of files from indexing**: You can use regular expressions to exclude certain files from indexing. For example, to exclude Microsoft Excel files, you can use a regular expression like this: `.*\.xls`
* **Mime-type detection**: You can use regular expressions to turn on "mime-type detection" for certain files, meaning that DocFetcher will try to detect their actual file types not just by looking at the filename, but also by peeking into the file contents. This comes in handy for files that have the wrong file extension.
* **Powerful query syntax**: In addition to basic constructs like `OR`, `AND` and `NOT` DocFetcher also supports, among other things: Wildcards, phrase search, fuzzy search ("find words that are similar to..."), proximity search ("these two words should be at most 10 words away from each other"), boosting ("increase the score of documents containing...")

Supported Document Formats
==========================
* Microsoft Office (doc, xls, ppt)
* Microsoft Office 2007 and newer (docx, xlsx, pptx, docm, xlsm, pptm)
* Microsoft Outlook (pst)
* OpenOffice.org (odt, ods, odg, odp, ott, ots, otg, otp)
* Portable Document Format (pdf)
* HTML (html, xhtml, ...)
* Plain text (customizable)
* Rich Text Format (rtf)
* AbiWord (abw, abw.gz, zabw)
* Microsoft Compiled HTML Help (chm)
* Microsoft Visio (vsd)
* Scalable Vector Graphics (svg)

What Some People Think About This Program...
============================================
${awards_table}

The user ratings on [our SourceForge.net page](http://sourceforge.net/projects/docfetcher/) also indicate that some people like our little program.

How Indexing Works
==================
**The naive approach to file search**: The most basic approach to file search is to simply visit every file in a certain location one-by-one whenever a search is performed. This works well enough for *filename-only* search because analyzing filenames is very fast. However, it wouldn't work so well if you wanted to search the *contents* of files, since full text extraction is a much more expensive operation than filename analysis.

**Index-based search**: That's why DocFetcher, being a content searcher, takes an approach known as *indexing*: The basic idea is that most of the files people need to search in (like, more than 95%) are modified very infrequently or not at all, so rather than doing full text extraction on every file on every search, it is far more efficient to perform text extraction on all files just *once*, and to create a so-called *index* from all the extracted text. This index is kind of like a dictionary that allows quickly looking up files by the words they contain.

**Telephone book analogy**: As an analogy, consider how much more efficient it is to look up someone's phone number in a telephone book (the "index") instead of calling *every* possible phone number just to find out whether the person on the other end is the one you're looking for. &mdash; Calling someone over the phone and extracing text from a file can both be considered "expensive operations". Also, the fact that people don't change their phone numbers very frequently is analogous to the fact that most files on a computer are rarely if ever modified.

**Index updates**: Of course, an index only reflects the state of the indexed files when it was created, not necessarily the latest state of the files. Thus, if the index isn't kept up-to-date, you could get outdated search results, much in the same way a telephone book can become out of date. However, this shouldn't be much of a problem if we can assume that most of the files are rarely modified. Additionally, DocFetcher is capable of *automatically* updating its indexes: (1) When it's running, it detects changed files and updates its indexes accordingly. (2) When it isn't running, a small daemon in the background will detect changes and keep a list of indexes to be updated; DocFetcher will then update those indexes the next time it is started.

Portable Document Repositories
==============================
**Portable document repositories**: There are of course other search programs out there that rely on indexing, notable examples being Windows Desktop Search and Google Desktop Search. However, DocFetcher goes one step further: There's a portable version of it that allows you to create a *portable document repository* &mdash; a fully indexed and fully searchable repository of all your important documents that you can freely move around.

**Usage examples**: There are all kinds of things you can do with this repository: You can carry it with you on a USB drive, burn it onto a CD-ROM for archiving purposes, put it in an encrypted volume (recommended: [TrueCrypt](http://www.truecrypt.org/)), synchronize it between multiple computers via a service like [DropBox](http://www.dropbox.com/), etc. Better yet, since DocFetcher is open source, you can even redistribute your repository: Upload it and share it with the rest of the world if you want.

**Java: Performance and portability**: One aspect some people might take issue with is that DocFetcher was written in Java, which has a reputation of being "slow". You know what? That was true *ten years ago*. [Read it up on Wikipedia](http://en.wikipedia.org/wiki/Java_%28software_platform%29#Performance) if you don't believe me. Anyways, the great thing about being written in Java is that the portable version of DocFetcher runs on Windows, Linux *and* Mac OS X. This means, for example, that you can put your portable document repository on a USB drive and then access it from *any* of these operating systems, provided that a Java runtime is installed.

Comparison To Other Desktop Search Applications
===============================================
DocFetcher was borne out of the observation that desktop search applications generally seem to fall into one of these two categories: (1) A supposedly "powerful" commercial program that slows down your computer and is filled with crap that nobody uses, all wrapped up in a clumsy interface. (2) A free program that has bugs instead of features, and that hasn't been updated since 2005.

We believe it is entirely possible for a desktop search application to fall into *neither* category. So here it is: An Open Source program with a compact, crap-free interface that provides most people with what they need, *without* installing any silly toolbars in your browser, and *without* harvesting your private data.
