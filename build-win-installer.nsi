; NSIS script for building the Windows installer
;
; This must be run after running build.py, because it expects to find the
; DocFetcher jar in the build folder.
;
; When building a new release, remember to update the version number in the next
; command.
;
; DEPENDENCIES
; http://nsis.sourceforge.net/Processes_plug-in
; http://nsis.sourceforge.net/Inetc_plug-in
; http://nsis.sourceforge.net/Java_Runtime_Environment_Dynamic_Installer
; If you get errors in JREDyna_Inetc just delete CUSTOM_PAGE_JREINFO
;


RequestExecutionLevel admin ; without this, the startmenu links won't be removed on Windows Vista/7
SetCompress force
SetCompressor /FINAL /SOLID lzma
SetCompressorDictSize 32

!define /file VERSION "current-version.txt"
!define PORTABLE_PATH build\DocFetcher-${VERSION}
!define JRE_VERSION "1.6"
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=52252"
!include "JREDyna_Inetc.nsh"

Name "DocFetcher ${VERSION}"
XPStyle on
OutFile build\docfetcher_${VERSION}_win32_setup.exe
InstallDir $PROGRAMFILES\DocFetcher
Page directory
Page instfiles
Page custom finalPage
UninstPage uninstConfirm
UninstPage instfiles

AllowSkipFiles off
ShowInstDetails show
ShowUninstDetails show
AutoCloseWindow true

!addplugindir dev
!include "FileFunc.nsh"
!include "nsDialogs.nsh"
!insertmacro GetTime

; Follow the list of languages on the wiki:
; http://sourceforge.net/apps/mediawiki/docfetcher/index.php?title=How_to_translate_DocFetcher#Translations_that_are_already_finished_or_in_progress
LoadLanguageFile "${NSISDIR}\Contrib\Language files\English.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Portuguese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\German.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Romanian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\French.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Russian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Greek.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Japanese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\SimpChinese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\TradChinese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Spanish.nlf"

Function .onInit
	startinst:
	Processes::FindProcess "DocFetcher.exe"
	StrCmp $R0 0 done
	MessageBox MB_RETRYCANCEL|MB_ICONEXCLAMATION \
		"     DocFetcher is running! $\n$\nPlease close all open instances before upgrading." \
	IDRETRY startinst
	Abort
	done:
FunctionEnd

Var CHECKBOX
Var boolCHECKBOX
Var Image
Var ImageHandle

; --------------------------------
; The final install page that asks to run the application
Function finalPage
	IfRebootFlag 0 noreboot
		MessageBox MB_YESNO "A reboot is required to finish the installation.$\n$\n Do you wish to reboot now?" IDNO endfinalpage
			Reboot
	noreboot:
	nsDialogs::Create 1018
	Pop $0
	${NSD_CreateLabel} 75u 30u 80% 8u "DocFetcher was succesfully installed on your computer."
	Pop $0
	${NSD_CreateCheckbox} 80u 50u 50% 8u "Run DocFetcher v${VERSION}"
	Pop $CHECKBOX
	SendMessage $CHECKBOX ${BM_SETCHECK} ${BST_CHECKED} 0
	GetFunctionAddress $1 OnCheckbox
	nsDialogs::OnClick $CHECKBOX $1

	; Add an image
	${NSD_CreateBitmap} 0 0 100% 40% ""
	Pop $Image
	${NSD_SetImage} $Image "$INSTDIR\img\setup.bmp" $ImageHandle
	nsDialogs::Show
	${NSD_freeImage} $ImageHandle
	endfinalpage:
FunctionEnd
Function OnCheckbox
	SendMessage $CHECKBOX ${BM_GETSTATE} 0 0 $1
	${If} $1 != 520
		StrCpy $boolCHECKBOX "True"
	${Else}
		StrCpy $boolCHECKBOX "False"
	${EndIf}
FunctionEnd
Function .onInstSuccess
	IfRebootFlag endpage 0
	${If} $boolCHECKBOX != "False"
		Exec "$INSTDIR\DocFetcher.exe"
	${EndIf}
	endpage:
FunctionEnd
Function .onInstFailed
    DetailPrint " --- "
    DetailPrint " Make sure DocFetcher is not running and try installation again "
    MessageBox MB_OK|MB_ICONEXCLAMATION "Please restart your computer and try the installation again."
FunctionEnd


Section "DocFetcher"
	SetShellVarContext all
	Call DownloadAndInstallJREIfNecessary
    killdaemon:
		Processes::FindProcess "docfetcher-daemon-windows"
		StrCmp $R0 0 nodaemon
		DetailPrint "Attempting to kill docfetcher-daemon..."
		Processes::KillProcess "docfetcher-daemon-windows"
		Sleep 250
    Goto killdaemon
    nodaemon:

	; Copy files
	SetOutPath $INSTDIR
	File /r "templates"
	File ${PORTABLE_PATH}\*.exe
	File ${PORTABLE_PATH}\*.txt

	SetOutPath $INSTDIR\misc
	File ${PORTABLE_PATH}\misc\*.bat
	File ${PORTABLE_PATH}\misc\*.exe
	; File ${PORTABLE_PATH}\misc\ChangeLog.html
	File ${PORTABLE_PATH}\misc\licenses.zip

	SetOutPath $INSTDIR\help
	File /r ${PORTABLE_PATH}\help\*.*

	SetOutPath $INSTDIR\img
	File /r ${PORTABLE_PATH}\img\*.*

	Delete /REBOOTOK "$INSTDIR\lib\net.sourceforge.docfetcher_*.*"
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
	CreateShortCut $SMPROGRAMS\DocFetcher\Readme.lnk $INSTDIR\Readme.txt
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
	Delete /REBOOTOK $INSTDIR\DocFetcher.exe
	Delete /REBOOTOK $INSTDIR\uninstaller.exe
	Delete /REBOOTOK $INSTDIR\docfetcher-daemon-windows.exe
	Delete /REBOOTOK $INSTDIR\Readme.txt
	Delete /REBOOTOK $INSTDIR\hs_err_pid*.log
	RMDir /r /REBOOTOK $INSTDIR\misc
	RMDir /r /REBOOTOK $INSTDIR\help
	RMDir /r /REBOOTOK $INSTDIR\img
	RMDir /r /REBOOTOK $INSTDIR\lib
	RMDir $INSTDIR

	; Remove registry key
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher"
	DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "DocFetcher-Daemon"

	SetShellVarContext current

	; Remove application data folder
	RMDir /r /REBOOTOK $APPDATA\DocFetcher

	; Remove start menu entries
	Delete /REBOOTOK $SMPROGRAMS\DocFetcher\DocFetcher.lnk
	Delete /REBOOTOK "$SMPROGRAMS\DocFetcher\Uninstall DocFetcher.lnk"
	Delete $SMPROGRAMS\DocFetcher\Readme.lnk
	; Delete $SMPROGRAMS\DocFetcher\ChangeLog.lnk
	RMDir $SMPROGRAMS\DocFetcher
SectionEnd
