介绍
============

DocFetcher是一个免费开源且跨平台的桌面文档内容搜索工具：它能遍历你所有的文件内容，进行全文搜索，类似百度硬盘或Google硬盘。

**基于索引**: 由于大量负载情况下直接搜索文档很缓慢，DocFetcher需要创建索引来加速搜索。不同于百度硬盘和Google硬盘，你可以针对自己希望搜索的文件夹建立索引。初次创建索引可能花费一些时间，但是一旦建立好就可以重复使用。

**创建索引**: 启动软件后，在右下角的*搜索范围*区域里右键，选择`从...创建索引`来选择要索引的文件夹，按`执行`确定并开始索引（文件数较多的话可能要等一段时间），当索引完成之后就可以进行搜索了！

**搜索**: 在搜索栏输入要搜索的内容并敲击回车即可查询。

*如果您正在阅读DocFetcher内的本手册，按照下一段中的说明将使手册消失。要恢复它，点击右上角的''？'`按钮。您也可以通过点击该窗格正上方的“在外部浏览器中打开”按钮，在默认网页浏览器中打开该手册。*

**结果窗口和预览窗口**: 在结果窗格下方（或者在其右侧，取决于当前的GUI布局），您可以找到预览窗格。如果您在结果窗格中选择一个文件，预览窗格将显示文件内容的纯文本预览。预览窗口具有下面功能：

* ***高亮***: 默认情况下，您输入的搜索字词将突出显示，您可以使用向上和向下按钮从一个事件跳转到上一个或下一个出现位置。
* ***内置的web浏览器***: 对于HTML文件，您可以在纯文本视图和简单的内置Web浏览器之间切换。 （注意：后者在某些Linux变体上不可用。）

一些快捷操作: 按下“Ctrl + F”或“Alt + F”将焦点移回搜索字段。要在外部程序中打开文件，请在结果窗格中双击该文件。

**排序**: 您可以通过单击任何结果窗格的列标题来更改结果的排序。例如，要按文件名对结果进行排序，请单击“文件名”标题。单击相同的标题两次将按相反的顺序排序。您也可以通过拖放操作来更改列的顺序：例如，如果您希望将“Filename”作为第一列，只需将“Filename”列标题拖到左侧即可。

**过滤**: 在GUI的左侧，您可以看到用于过滤结果的各种控件：（1）您可以在“最小/最大文件大小”控件中指定最小和/或最大文件大小。 （2）“文档类型”列表允许您按类型过滤结果。 （3）通过取消选中“搜索范围”区域中的项目，可以按位置过滤结果。

**更新索引**: 如果索引文件夹中的文件被添加，删除或修改，则相应的索引必须更新，否则您的搜索结果可能会过时。幸运的是，更新索引实际上总是比从头创建索引要快得多，因为只有更改需要处理。另外，DocFetcher可以通过两种方式自动更新其索引：

1. ***DocFetcher 主程序***: 如果DocFetcher正在运行，并且启用了`监视文件夹中文件的变化`，则DocFetcher会检测更改并立即更新其索引。
2. ***DocFetcher 守护程序***: 如果DocFetcher未运行，则更改将由在后台运行的小型守护程序记录;受影响的索引将在下次DocFetcher启动时更新。 （注意：不幸的是，守护进程目前在Mac OS X上不可用。）

一些注意事项：如果您使用DocFetcher的可移植版本并希望运行守护程序，则必须通过将守护程序可执行文件添加到操作系统的启动程序列表中来手动安装它。另外，DocFetcher和守护进程都不能检测网络共享上的变化。
因此，在索引无法自动更新的情况下，您必须自己完成：在“搜索范围”区域中，选择一个或多个要更新的索引，然后单击“更新索引” '搜索范围'上下文菜单，或者按'F5`键。

* * *

<a name="Advanced_Usage"></a> <!-- Do not translate this line, just copy it verbatim. -->

高级用法
==============

**查询语法**: 借助DocFetcher，您可以做的不仅仅是简单的单词查询。例如，您可以使用通配符搜索具有共同开始的单词，如下所示：`wiki*`。要搜索某个短语（即按特定顺序排列的单词序列），请用引号括住该短语：`"the quick brown fox"`。但这仅仅是一个开始。有关所有受支持的构造的概述，请参见[查询语法部分]（DocFetcher_Manual_files / Query_Syntax.html）。

**首选项**: 在用户界面的右上角，您会看到一个描绘两个齿轮的图标。点击它打开首选项对话框。可以通过首选项对话框左下角的“高级设置”链接访问更高级用法的其他设置。

**Portable document repository**: The portable version of DocFetcher allows you create a bundle containing DocFetcher, your documents and the associated indexes, and then freely move this bundle around &mdash; even from one operating system to another, e.g. from Windows to Linux and vice versa. One important thing to keep in mind when using the portable version is that the indexes must be created with *relative paths*. Click [here](DocFetcher_Manual_files/Portable_Repositories.html) for more information about portable document repositories. By the way, if you've been using DocFetcher 1.0.3 and earlier, note that you're not required to put your documents into the DocFetcher folder anymore.

**Indexing configuration options**: For a detailed discussion of all those options on the indexing configuration window, click [here](DocFetcher_Manual_files/Indexing_Options.html). You can also reach this manual page directly from the configuration window by clicking on the `Help` button at the bottom of the window. Perhaps the most interesting configuration options are:

* ***Customizable file extensions***: The file extensions for plain text files and zip archives are fully customizable. This is particularly useful for indexing source code files.
* ***File exclusion***: You can exclude certain files from indexing based on regular expressions.
* ***Mime type detection***: Without mime type detection, DocFetcher will just look at a file's extension (e.g. `'.doc'`) to determine its file type. With mime type detection, DocFetcher will also peek into the file's contents to see if it can find any better type info. This is slower than just checking the file extension, but it's useful for files that have the wrong file extension.
* ***HTML pairing***: By default, DocFetcher treats an HTML file and its associated folder (e.g. a file `foo.html` and a folder `foo_files`) as a single document. The main purpose of this is to make all the "clutter" inside the HTML folders disappear from the search results.

**Regular expressions**: Both the file exclusion and the mime type detection rely on so-called *regular expressions*. These are user-defined patterns that DocFetcher will match against filenames or filepaths. For example, to exclude all files starting with the word "journal", you can use this regular expression: `journal.*`. Note that this is slightly different from DocFetcher's query syntax, where you would omit the dot: `journal*`. If you want to know more about regular expressions, read this [brief introduction](DocFetcher_Manual_files/Regular_Expressions.html).

**Release notification**: DocFetcher does not (and should not?) automatically check for updates. If you *do* wish to be notified of new releases, there are [a couple of ways](DocFetcher_Manual_files/Release_Notification.html) to set this up.

* * *

<a name="Caveats"></a> <!-- Do not translate this line, just copy it verbatim. -->

Caveats and Common Gotchas
==========================

**Raising the memory limit**: DocFetcher, like all Java programs, has a fixed limit on how much memory it's allowed to use, known as the *Java heap size*. This memory limit must be set on startup, and DocFetcher currently chooses a default value of 256&nbsp;MB. If you try to index a very, very large number of files, and/or if some of the indexed files are really huge (which is not uncommon with PDF files), then chances are DocFetcher will hit that memory limit. If this ever happens, you might want to [raise the memory limit](DocFetcher_Manual_files/Memory_Limit.html).

**Don't index system folders**: In contrast to other desktop search applications, DocFetcher was not designed for indexing system folders such as `C:`or `C:\Windows`. Doing so is discouraged for the following reasons:

1. ***Slowdown***: The files in system folders tend to be modified very frequently. If the folder watching is turned on, this will cause DocFetcher to update its indexes all the time, slowing down your computer.
2. ***Memory issues***: DocFetcher needs to keep tiny representations of your files in memory. Because of this, and because system folders usually contain a very large number of files, DocFetcher will be more likely to run out of memory if you index system folders.
3. ***Waste of resources, worse search results***: Apart from these technical reasons, indexing system folders is most likely a waste of indexing time and disk space, and it will also pollute your search results with unneeded system files. So, for the best results in the least amount of time, just index what you need.

**Unicode support**: DocFetcher has full Unicode support for all document formats. In case of plain text files, DocFetcher has to use [certain heuristics](http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html) to guess the correct encoding, since plain text files don't contain any explicit encoding information.

**Archive support**: DocFetcher currently supports the following archive formats: zip and derived formats, 7z, rar and the whole tar.* family. Additionally, executable zip and 7z archives are supported as well, but not executable rar archives. DocFetcher will treat all archives as if they were ordinary folders, and it can also handle an arbitrarily deep nesting of archives (e.g. a zip archive containing a 7z archive containing a rar archive...).<!-- this line should end with two spaces -->  
With that said, it should be noted that support for zip and 7z archives is best in terms of robustness and speed. On the other hand, indexing of tar.gz, tar.bz2 and similar formats tends to be less efficient. This is due to the fact that these formats don't have an internal "summary" of the archive contents, which forces DocFetcher to unpack the entire archive rather than only individual archive entries. Bottom line: If you have the choice, compress your files either as zip or 7z archives for maximum compatibility with DocFetcher.

**The DocFetcher daemon is innocent**: If you suspect that the DocFetcher daemon is slowing down your computer or causing crashes, you're probably wrong. As a matter of fact, the daemon is a very simple program with low memory footprint and CPU usage, and it doesn't do much besides watching folders. If you're still not convinced, just rename the daemon executables so they won't start automatically, or try the portable version of DocFetcher, where the daemon is deactivated by default.

* * *

<a name="Subpages"></a> <!-- Do not translate this line, just copy it verbatim. -->

Manual Subpages
===============
* [Query syntax](DocFetcher_Manual_files/Query_Syntax.html)
* [Portable document repositories](DocFetcher_Manual_files/Portable_Repositories.html)
* [Indexing options](DocFetcher_Manual_files/Indexing_Options.html)
* [Regular expressions](DocFetcher_Manual_files/Regular_Expressions.html)
* [Release notification](DocFetcher_Manual_files/Release_Notification.html)
* [How to raise the memory limit](DocFetcher_Manual_files/Memory_Limit.html)
* [How to raise the folder watch limit (Linux)](DocFetcher_Manual_files/Watch_Limit.html)
* [Preferences](DocFetcher_Manual_files/Preferences.html)

Further Information
===================
For more information, have a look at our [wiki](http://docfetcher.sourceforge.net/wiki/doku.php). If you have any questions, feel free to visit our [forum](http://sourceforge.net/projects/docfetcher/forums/forum/702424). Bug reports can be submitted on our [bug tracker](http://sourceforge.net/tracker/?group_id=197779&atid=962834).
