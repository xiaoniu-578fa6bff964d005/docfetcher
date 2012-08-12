How to raise the memory limit
=============================
DocFetcher has a default memory limit of 256 MB, which is set on startup. The limit can be raised by fiddling with the platform-specific launchers:

Windows
-------
The Windows version of DocFetcher ships with ready-made alternative launchers that set different heap sizes. Follow these steps to use them:

* Open the DocFetcher folder. If you're using the portable version of DocFetcher, this is just the folder you downloaded. If you're using the non-portable version, the DocFetcher folder will be in `C:\Program Files\`, or `C:\Program Files (x86)`, or a similar location.
* The alternative launchers are inside the `DocFetcher\misc` folder. They are named `DocFetcher-XXX.exe`, where `XXX` is the heap size set by the respective launcher. For example, the launcher `DocFetcher-512.exe` will set a heap size of 512 MB.
* Before you can use any of these launchers, **you must first move or copy it into the DocFetcher folder**. It's not necessary to delete the default launcher or to rename the alternative launcher.

Another way of changing the memory limit is to copy the file `misc\DocFetcher.bat` into the DocFetcher folder and alter the expression `-Xmx256m` in the last line of the file, for example to `-Xmx512m`.

Linux
-----
Open the launcher script `DocFetcher/DocFetcher.sh` with a text editor, and in the last line, alter the expression `-Xmx256m` as needed, for example to `-Xmx512m`.

Mac OS&nbsp;X
-------------
Both in the non-portable and the portable version, DocFetcher is launched via an application bundle. In the non-portable version, the application bundle is just what you got out of the downloaded disk image. In the portable version, the application bundle can be found in the DocFetcher folder.

In both cases, the application bundle is actually a folder with the extension `.app`. There should be a context menu entry in Finder to open this folder. If your Mac OS&nbsp;X language is English, this menu entry will be named `Show Package Contents`.

Inside the folder, you will find this launcher script: `Contents/MacOS/DocFetcher`. Open it with a text editor, and in the last line, alter the expression `-Xmx256m` as needed, for example to `-Xmx512m`.
