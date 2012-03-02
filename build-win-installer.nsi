; NSIS script for building the Windows installer
;
; This must be run after running build.py, because it expects to find the
; DocFetcher jar in the build folder.
;
; When building a new release, remember to update the version number in the next
; command.

!define VERSION 1.1-beta6
!define PORTABLE_PATH build\DocFetcher-${VERSION}

RequestExecutionLevel admin ; without this, the startmenu links won't be removed on Windows Vista/7
SetCompress force
SetCompressor /SOLID lzma
Name "DocFetcher ${VERSION}"
XPStyle on
OutFile build\docfetcher_${VERSION}_win32_setup.exe
InstallDir $PROGRAMFILES\DocFetcher
Page directory
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles
AutoCloseWindow true

!addplugindir dev
!include "FileFunc.nsh"
!insertmacro GetTime

LoadLanguageFile "${NSISDIR}\Contrib\Language files\Afrikaans.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Albanian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Arabic.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Basque.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Belarusian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Bosnian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Breton.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Bulgarian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Catalan.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Croatian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Czech.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Danish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Dutch.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\English.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Estonian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Farsi.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Finnish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\French.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Galician.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\German.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Greek.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Hebrew.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Hungarian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Icelandic.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Indonesian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Irish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Italian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Japanese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Korean.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Kurdish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Latvian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Lithuanian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Luxembourgish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Macedonian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Malay.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Mongolian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Norwegian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\NorwegianNynorsk.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Polish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Portuguese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\PortugueseBR.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Romanian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Russian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Serbian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\SerbianLatin.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\SimpChinese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Slovak.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Slovenian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Spanish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\SpanishInternational.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Swedish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Thai.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\TradChinese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Turkish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Ukrainian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Uzbek.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Welsh.nlf"

Function .onInit
	ReadRegStr $R0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher" "UninstallString"
	StrCmp $R0 "" done
	MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION \
	"DocFetcher is already installed. $\n$\nClick 'OK' to remove the \
	previous version or 'Cancel' to cancel this upgrade." \
	IDOK uninst
	Abort
	
	uninst:
		ClearErrors
		ExecWait '$R0 /S _?=$INSTDIR'
		IfErrors no_remove_uninstaller
			Delete $INSTDIR\uninstaller.exe
			RMDIR $INSTDIR
		no_remove_uninstaller:
	done:
FunctionEnd

Section "DocFetcher"
	SetShellVarContext all

	; Copy files
    SetOutPath $INSTDIR
    File ${PORTABLE_PATH}\*.exe
    ; File ${PORTABLE_PATH}\*.txt
	
	SetOutPath $INSTDIR\misc
	File ${PORTABLE_PATH}\misc\*.bat
	File ${PORTABLE_PATH}\misc\*.exe
	; File ${PORTABLE_PATH}\misc\ChangeLog.html
	File ${PORTABLE_PATH}\misc\licenses.zip
	
	SetOutPath $INSTDIR\help
    File /r ${PORTABLE_PATH}\help\*.*
    
    SetOutPath $INSTDIR\img
    File /r ${PORTABLE_PATH}\img\*.*
    
    SetOutPath $INSTDIR\lib
    File /r /x *.so /x *.dylib /x *linux* /x *macosx* /x *docfetcher*.jar ${PORTABLE_PATH}\lib\*.*
	File build\tmp\net.sourceforge.docfetcher*.jar
    
    ; Uninstaller
    WriteUninstaller $INSTDIR\uninstaller.exe
    
    ; Write to registry
    Var /GLOBAL regkey
    Var /GLOBAL homepage
    StrCpy $regkey "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher"
    StrCpy $homepage "http://docfetcher.sourceforge.net"
    WriteRegStr HKLM $regkey "DisplayName" "DocFetcher"
    WriteRegStr HKLM $regkey "UninstallString" "$INSTDIR\uninstaller.exe"
    WriteRegStr HKLM $regkey "InstallLocation" $INSTDIR
    WriteRegStr HKLM $regkey "DisplayIcon" "$INSTDIR\DocFetcher.exe,0"
    WriteRegStr HKLM $regkey "HelpLink" $homepage
    WriteRegStr HKLM $regkey "URLUpdateInfo" $homepage
    WriteRegStr HKLM $regkey "URLInfoAbout" $homepage
    WriteRegStr HKLM $regkey "DisplayVersion" "${VERSION}"
    WriteRegDWORD HKLM $regkey "NoModify" 1
    WriteRegDWORD HKLM $regkey "NoRepair" 1
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "DocFetcher-Daemon" "$INSTDIR\docfetcher-daemon-win.exe"
	
	SetShellVarContext current
	
	; Start menu entries
    CreateDirectory $SMPROGRAMS\DocFetcher
    CreateShortCut $SMPROGRAMS\DocFetcher\DocFetcher.lnk $INSTDIR\DocFetcher.exe
    CreateShortCut "$SMPROGRAMS\DocFetcher\Uninstall DocFetcher.lnk" $INSTDIR\uninstaller.exe
    ; CreateShortCut $SMPROGRAMS\DocFetcher\Readme.lnk $INSTDIR\Readme.txt
    ; CreateShortCut $SMPROGRAMS\DocFetcher\ChangeLog.lnk $INSTDIR\misc\ChangeLog.html
	
	; Launch daemon
	Exec '"$INSTDIR\docfetcher-daemon-windows.exe"'
SectionEnd

Section "un.Uninstall"
	SetShellVarContext all

	; Kill daemon
	Processes::KillProcess "docfetcher-daemon-windows"
	Sleep 1000
	
	; Remove program folder
	Delete $INSTDIR\DocFetcher.exe
	Delete $INSTDIR\uninstaller.exe
	Delete $INSTDIR\docfetcher-daemon-windows.exe
	Delete $INSTDIR\Readme.txt
    Delete $INSTDIR\hs_err_pid*.log
	RMDir /r $INSTDIR\misc
	RMDir /r $INSTDIR\help
    RMDir /r $INSTDIR\img
	RMDir /r $INSTDIR\lib
	RMDir $INSTDIR
	
	; Remove registry key
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher"
    DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "DocFetcher-Daemon"
	
	SetShellVarContext current
	
	; Remove application data folder
    RMDir /r $APPDATA\DocFetcher
	
	; Remove start menu entries
	Delete $SMPROGRAMS\DocFetcher\DocFetcher.lnk
	Delete "$SMPROGRAMS\DocFetcher\Uninstall DocFetcher.lnk"
	; Delete $SMPROGRAMS\DocFetcher\Readme.lnk
	; Delete $SMPROGRAMS\DocFetcher\ChangeLog.lnk
	RMDir $SMPROGRAMS\DocFetcher
SectionEnd
