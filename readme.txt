#===========================================================
#	Setup in Eclipse
#===========================================================
- Get Eclipse classic edition (tested with 3.6 and later)
- Install AJDT plugin (needed for some AspectJ code portions)
- Import the DocFetcher folder into your Eclipse workspace
- In Eclipse, create a User Library named 'SWT' which points to the right SWT jar for your platform. The SWT jars can be found in lib/swt.
- Main class: net.sourceforge.docfetcher.gui.Application
- Required VM arguments
	Windows:
		-Djava.library.path="lib/chm4j;lib/jnotify;lib/jintellitype"
	Linux:
		-Djava.library.path="lib/chm4j:lib/jnotify:lib/jxgrabkey"
	Mac OS X:
		-Djava.library.path="lib/jnotify"
		-XstartOnFirstThread
- Optional VM argument: -enableassertions


#===========================================================
#	Building DocFetcher from the console
#===========================================================
- Requirements: Python and JDK 6.0+
- current-version.txt:
	- this file contains the version number used by all build scripts below
	- this file must not contain any extra whitespace or newlines
- build.py:
	- the main build file that builds DocFetcher
	- output is in the "build" folder
- build-win-installer.nsi
	- NSIS script for building the Windows installer
	- requires NSIS and must be run on Windows
	- must run build.py first
	- output is in the "build" folder
- build-man.py:
	- recreates the manual
- build-website.py:
	- recreates the website
- build-dmg.sh:
	- builds a Mac OS X disk image
	- must run build.py first
	- must be run on Linux
	- output is in the "build" folder
- build-daemon.xml:
	- Ant file for building the DocFetcher daemon
	- probably stopped working a long time ago
