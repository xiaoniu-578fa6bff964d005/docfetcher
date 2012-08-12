Portable Document Repositories
==============================

Basic usage
-----------
The portable version of DocFetcher essentially allows you to carry around (and even redistribute) a fully indexed and fully searchable document repository. If you don't have the portable version yet, you can download it from the [project website](http://docfetcher.sourceforge.net/download.html).

The portable version does not require any installation; just extract the contents of the archive into a folder of your choice. You can then start DocFetcher via the appropriate launcher for your operating system: `DocFetcher.exe` on Windows, `DocFetcher.sh` on Linux and the `DocFetcher` application bundle on Mac OS&nbsp;X. The only requirement is that a Java runtime, version 1.6 or newer, must be installed on the machine.

<u>Relative paths</u>: An important thing to pay attention to is that all indexes must be created with the *relative paths* setting turned on. Without this, DocFetcher will store *absolute* references to your files, so you will only be able to move DocFetcher and its indexes around, but not your files &mdash; at least not without breaking references. Here's an example to illustrate this:

* Relative path: `..\..\my-files\some-document.txt`
* Absolute path: `C:\my-files\some-document.txt`

The relative path basically tells DocFetcher that it can find `some-document.txt` by going up two levels from its current location and then down into the `my-files` folder. The absolute path on the other hand is a fixed reference and independent of DocFetcher's current location, so you can't move `some-document.txt` without breaking the reference (meaning DocFetcher won't be able to locate the file).

Note that DocFetcher can only *attempt* to store relative paths: Obviously, it can't do so if you put DocFetcher and your files on different volumes, e.g. DocFetcher in `D:\DocFetcher` and your files in `E:\my-files`.

Usability tips
--------------

* ***CD-ROM archiving***: Just common sense, but still: If you put DocFetcher on a CD-ROM, you won't be able to save changes to the preferences or the indexes, so remember to properly configure everything before burning it onto the CD-ROM. Also, you might want to include a Java runtime installer.
* ***Different program titles***: For redistribution of your portable document repository, or to make working with multiple DocFetcher instances less confusing, you can give each DocFetcher instance a different program window title. To do so, change the `AppName` setting in the file `program.conf`, which can be found in the `conf` folder.

Warnings
--------

* ***Don't touch the `indexes` folder***: You may, but are not required to put your files directly into the DocFetcher folder. If you do, leave the `indexes` folder alone, because anything you put in it might become deleted!
* ***Filename incompatibilities***: Beware of filename incompatibilities between different operating systems. For example, on Linux filenames can contain characters such as ":" or "|", but on Windows they can't. As a result, you can only move a document repository from Linux to Windows or in the opposite direction if it doesn't contain documents with incompatible filenames. Oh, and special characters such as German umlauts are an entirely different matter...
