介绍
============

DocFetcher是一个免费开源且跨平台的桌面文档内容搜索工具：它能遍历你所有的文件内容，进行全文搜索，类似百度硬盘或Google硬盘。

**基于索引**: 由于大量负载情况下直接搜索文档很缓慢，DocFetcher需要创建索引来加速搜索。不同于百度硬盘和Google硬盘，你可以针对自己希望搜索的文件夹建立索引。初次创建索引可能花费一些时间，但是一旦建立好就可以重复使用。

**创建索引**: 启动软件后，在右下角的*搜索范围*区域里右键，选择`从...创建索引`来选择要索引的文件夹，按`执行`确定并开始索引（文件数较多的话可能要等一段时间），当索引完成之后就可以进行搜索了！

**搜索**: 在搜索栏输入要搜索的内容并敲击回车即可查询。

*如果您正在阅读DocFetcher内的本手册，按照下一段中的说明将使手册消失。要恢复它，点击右上角的`？`按钮。您也可以通过点击该窗格正上方的“在外部浏览器中打开”按钮，在默认网页浏览器中打开该手册。*

**结果窗口和预览窗口**: 在结果窗格下方（或者在其右侧，取决于当前的GUI布局），您可以找到预览窗格。如果您在结果窗格中选择一个文件，预览窗格将显示文件内容的纯文本预览。预览窗口具有下面功能：

* ***高亮***: 默认情况下，您输入的搜索字词将突出显示，您可以使用向上和向下按钮从一个事件跳转到上一个或下一个出现位置。
* ***内置的web浏览器***: 对于HTML文件，您可以在纯文本视图和简单的内置Web浏览器之间切换。 （注意：后者在某些Linux变体上不可用。）

一些快捷操作: 按下“Ctrl + F”或“Alt + F”将焦点移回搜索字段。要在外部程序中打开文件，请在结果窗格中双击该文件。

**排序**: 您可以通过单击任何结果窗格的列标题来更改结果的排序。例如，要按文件名对结果进行排序，请单击“文件名”标题。单击相同的标题两次将按相反的顺序排序。您也可以通过拖放操作来更改列的顺序：例如，如果您希望将“文件名”作为第一列，只需将“文件名”列标题拖到左侧即可。

**过滤**: 在GUI的左侧，您可以看到用于过滤结果的各种控件：（1）您可以在“最小/最大文件大小”控件中指定最小和/或最大文件大小。 （2）“文档类型”列表允许您按类型过滤结果。 （3）通过取消选中“搜索范围”区域中的项目，可以按位置过滤结果。

**更新索引**: 如果索引文件夹中的文件被添加，删除或修改，则相应的索引必须更新，否则您的搜索结果可能会过时。幸运的是，更新索引实际上总是比从头创建索引要快得多，因为只有更改需要处理。另外，DocFetcher可以通过两种方式自动更新其索引：

1. ***DocFetcher 主程序***: 如果DocFetcher正在运行，并且启用了`监视文件夹中文件的变化`，则DocFetcher会检测更改并立即更新其索引。
2. ***DocFetcher 守护程序***: 如果DocFetcher未运行，则更改将由在后台运行的小型守护程序记录;受影响的索引将在下次DocFetcher启动时更新。 （注意：不幸的是，守护进程目前在Mac OS X上不可用。）

一些注意事项：如果您使用DocFetcher的可移植版本并希望运行守护程序，则必须通过将守护程序可执行文件添加到操作系统的启动程序列表中来手动安装它。另外，DocFetcher和守护进程都不能检测网络共享上的变化。<!-- this line should end with two spaces -->  
因此，在索引无法自动更新的情况下，您必须自己完成：在“搜索范围”区域中，选择一个或多个要更新的索引，右键菜单中点击`更新索引`，或者按'F5`键。

* * *

<a name="Advanced_Usage"></a> <!-- Do not translate this line, just copy it verbatim. -->

高级用法
==============

**查询语法**: 借助DocFetcher，您可以做的不仅仅是简单的单词查询。例如，您可以使用通配符搜索具有共同开始的单词，如下所示：`wiki*`。要搜索某个短语（即按特定顺序排列的单词序列），请用引号括住该短语：`"the quick brown fox"`。但这仅仅是一个开始。有关所有受支持的构造的概述，请参见[查询语法部分](DocFetcher_Manual_files/Query_Syntax.html)。

**首选项**: 在用户界面的右上角，您会看到一个描绘两个齿轮的图标。点击它打开首选项对话框。可以通过首选项对话框左下角的“高级设置”链接访问更高级用法的其他设置。

**可移植文档库**: DocFetcher的可移植版本允许您创建一个包含DocFetcher，您的文档和相关索引的捆绑包，然后自由移动此捆绑包&mdash;甚至从一个操作系统到另一个操作系统从Windows到Linux，反之亦然。使用可移植版本时需要牢记的一点是索引必须使用*相对路径*来创建。单击[here](DocFetcher_Manual_files/Portable_Repositories.html)以获取有关可移植文档存储库的更多信息。如果您一直使用DocFetcher 1.0.3及更早的版本，请注意，您不需要将文档放入DocFetcher文件夹中。

**索引配置选项**:有关索引配置窗口中所有这些选项的详细讨论，请单击[here](DocFetcher_Manual_files/Indexing_Options.html)。您也可以通过单击窗口底部的“帮助”按钮直接从配置窗口访问此手册页。也许最有趣的配置选项是：

* ***可自定义的文件扩展名***：纯文本文件和zip文件的文件扩展名完全可自定义。这对索引源代码文件特别有用。
* ***文件排除***：您可以根据正则表达式从索引中排除某些文件。
* ***Mime类型检测***：如果没有MIME类型检测，DocFetcher只会查看文件的扩展名（例如`'.doc'`）来确定其文件类型。通过MIME类型检测，DocFetcher还可以查看文件内容以查看是否可以找到更好的类型信息。这比检查文件扩展名要慢，但对于文件扩展名错误的文件很有用。
* ***HTML配对***：默认情况下，DocFetcher将HTML文件及其关联文件夹（例如文件`foo.html`和文件夹`foo_files`）视为单个文档。这样做的主要目的是使HTML文件夹内的所有“混乱”从搜索结果中消失。

**正则表达式**：文件排除和MIME类型检测均依赖于所谓的*正则表达式*。这些是DocFetcher将与文件名或文件路径匹配的用户定义模式。例如，要排除以"journal"开头的所有文件，可以使用以下正则表达式：`journal.*`。请注意，这与DocFetcher的查询语法略有不同，您可能会忽略`'.'`：`journal*`。如果您想了解更多关于正则表达式的知识，请阅读[简介](DocFetcher_Manual_files/Regular_Expressions.html)。

**发布通知**：DocFetcher不会（也不应该）自动检查更新。如果您希望收到新版本的通知，可以通过[方法](DocFetcher_Manual_files/Release_Notification.html)进行设置。

* * *

<a name="Caveats"></a> <!-- Do not translate this line, just copy it verbatim. -->

警告和常见问题
==========================

**提高内存限制**：与所有Java程序一样，DocFetcher对允许使用多少内存有一个固定限制，称为*Java堆大小*。此内存限制必须在启动时设置，DocFetcher当前选择默认值256 MB。如果您尝试索引非常大量的文件，或者某些索引文件非常庞大（PDF文件并不罕见），那么DocFetcher可能会达到该内存限制。如果发生这种情况，您可能需要[提高内存限制](DocFetcher_Manual_files/Memory_Limit.html)。

**不索引系统文件夹**：与其他桌面搜索应用程序相比，DocFetcher并非专门用于索引系统文件夹，如`C:`或`C:\Windows`。由于以下原因，不鼓励这样做：

1. ***变慢***：系统文件夹中的文件往往会被非常频繁地修改。如果打开文件夹观看功能，这将导致DocFetcher始终更新其索引，从而减慢计算机速度。
2. ***内存问题***：DocFetcher需要将文件的微小表示保存在内存中。因此，并且由于系统文件夹通常包含大量文件，因此如果您为系统文件夹编制索引，则DocFetcher将更有可能耗尽内存。
3. ***资源浪费，搜索结果更差***：除了这些技术原因外，索引系统文件夹很可能浪费索引时间和磁盘空间，并且还会用不需要的系统文件污染您的搜索结果。因此，为了在最短的时间内取得最好的结果，只需索引你所需要的。

**Unicode支持**：DocFetcher对所有文档格式都有完全的Unicode支持。对于纯文本文件，DocFetcher必须使用[某些启发式方法](http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html)来猜测正确的编码，因为纯文本文件不包含任何明确的编码信息。

**压缩文件支持**：DocFetcher目前支持以下存档格式：zip和衍生格式，7z,rar和整个tar.*系列。此外，还支持可执行的zip和7z压缩文件，但不支持可执行的rar压缩文件。 DocFetcher会将所有存档视为普通文件夹，并且还可以处理任意深度的存档嵌套（例如，包含包含rar存档的7z存档的zip存档...）。<!-- this line should end with two spaces -->  
虽然Docfetcher支持多种压缩格式，应该指出的是，对于zip和7z压缩文件的支持在稳健性和速度方面是最好的。另一方面，tar.gz，tar.bz2和类似格式的索引往往效率较低。这是由于这些格式没有归档内容的内部“摘要”，这迫使DocFetcher解压缩整个归档文件而不是单个归档条目。所以如果您有选择，请将文件压缩为zip或7z存档，以最大程度地兼容DocFetcher。

**DocFetcher守护进程并不增加系统负担**：如果您怀疑DocFetcher守护进程正在减慢计算机速度或导致崩溃，那么您可能是错误的。事实上，守护进程是一个非常简单的程序，它占用的内存很少，CPU使用率也很低，除了监视文件夹之外，它并没有太多的工作。如果您仍然不确定，只需重命名守护程序可执行文件，以免它们自动启动，或尝试DocFetcher的可移植版本，默认情况下会禁用守护程序。

* * *

<a name="Subpages"></a> <!-- Do not translate this line, just copy it verbatim. -->

手册子页面
===============
* [搜索语法](DocFetcher_Manual_files/Query_Syntax.html)
* [可移植文档仓库](DocFetcher_Manual_files/Portable_Repositories.html)
* [索引设置](DocFetcher_Manual_files/Indexing_Options.html)
* [正则表达式](DocFetcher_Manual_files/Regular_Expressions.html)
* [新版本通知](DocFetcher_Manual_files/Release_Notification.html)
* [如何提高内存限制](DocFetcher_Manual_files/Memory_Limit.html)
* [如何提高文件夹的watch limit (Linux)](DocFetcher_Manual_files/Watch_Limit.html)
* [首选项](DocFetcher_Manual_files/Preferences.html)

更多
===================
有关更多信息，请查看我们的[wiki](http://docfetcher.sourceforge.net/wiki/doku.php)。如果您有任何问题，请随时访问我们的[论坛](http://sourceforge.net/projects/docfetcher/forums/forum/702424)。错误报告可以在我们的[bug跟踪器](http://sourceforge.net/tracker/?group_id=197779&atid=962834)上提交。
