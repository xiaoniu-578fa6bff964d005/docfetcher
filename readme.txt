#===========================================================
#	Setup in Eclipse
#===========================================================
- Get Eclipse (tested with v4.3, Java Developers edition)
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
	- requires NSIS plugins in dev/nsis-dependencies
	  (copy them into the plugins folder of your NSIS installation)
	- must run build.py first before running this
	- output is in the "build" folder
- build-man.py:
	- recreates the manual
	- output is in dist/help
- build-website.py:
	- recreates the website
	- output is in dist/website
- build-dmg.sh:
	- builds a Mac OS X disk image
	- must run build.py first
	- must be run on Linux
	- requires program mkfs.hfsplus (try package hfsprogs on Ubuntu)
	- output is in the "build" folder
- build-daemon.xml:
	- Ant file for building the DocFetcher daemon
	- probably stopped working a long time ago
- deploy-website.sh:
	- deploys the website to the project webspace on SourceForge.net
	- will automatically run build-website.py
	- must specify SourceForge.net user name and password


#===========================================================
#	The DocFetcher Launchers
#===========================================================
The DocFetcher launchers for all platforms can be found under dist/launchers.
The DocFetcher.exe launchers in that folder have been created with Launch4J,
according to the settings in dev/launch4j-config.txt, and using the icon file
dev/DocFetcher.ico.
