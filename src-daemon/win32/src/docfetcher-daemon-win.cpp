/*******************************************************************************
 * Copyright (c) 2009 Tonio Rush.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tonio Rush - initial API and implementation
 *******************************************************************************/

#include <iostream>
#include <sstream>

#include "windows.h"
#include "TCHAR.h"
#include "string.h"

#include <process.h>
#include <psapi.h>

#include "jnotify_win32/Logger.h"
#include "jnotify_win32/Win32FSHook.h"

#include "FolderWatcher.h"


/**
 * used by JNotify
 *
 */
extern bool dbg;

/**
 * Globals
 *
 */

HINSTANCE _hInstance;
HWND _hwndMain;
Win32FSHook *_win32FSHook;
FolderWatcher _folderWatcher;

/**
 * This module functions
 *
 */
LRESULT CALLBACK WndProc(HWND, UINT, WPARAM, LPARAM);
bool InitInstance();
void watchLoop(void *);
bool isUniqueInstance();

/**
 * Entry point
 *
 * installs the watches
 *
 */
int APIENTRY WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance,
		LPTSTR lpCmdLine, int nCmdShow) {
	UNREFERENCED_PARAMETER(lpCmdLine);

	log("docfetcher-daemon-win starting...");

	_hInstance = hInstance;

	// If the main window cannot be created, terminate the application.
	if(!InitInstance()){
		log("InitInstance failed");
		return 1;
	}

	// JNotify_win32 traces
	dbg = true;

	// find indexes.txt file
	if(!_folderWatcher.findIndexesFile()) {
		log("Cannot get indexes file.");
		return 1;
	}

	if(!isUniqueInstance()) {
		log("another instance is running...");
		return 1;
	}


	_win32FSHook = new Win32FSHook();
	_win32FSHook->init(NULL);

    _beginthread(watchLoop, 0, NULL);


	// Main message loop:
	MSG msg;
	while (GetMessage(&msg, NULL, 0, 0)) {
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}

	log("docfetcher-daemon-win exiting...");
	delete _win32FSHook;
	return 0;
}



/**
 * Watches regulary if the .lock file is opened by DocFetcher
 *
 * installs and removes the watches in consequence
 *
 */
void watchLoop(void *){
	std::string lock_file = _folderWatcher.getLockFile();
	log("lock file : %s", lock_file.c_str());
	bool watching = false;
	for(;;){
		::DeleteFile(lock_file.c_str());
		if(GetLastError() == ERROR_SHARING_VIOLATION){
			// the file is used by DocFetcher, so stop watching
			if(watching) {
				log("stopWatch");
				_folderWatcher.stopWatch();
				watching = false;
			}
		}else{
			// the file is not used by DocFetcher, so start watching
			if(!watching) {
				log("startWatch");
				_folderWatcher.startWatch();
				watching = true;
			}
		}

		// Check is done every 2 seconds
		::Sleep(2000);
	}
}

/**
 * Check if this is the only instance
 * It is done by creating a mutex with the daemon lockfile full path
 * to distinguish various installations of Docfetcher
 *
 */
bool isUniqueInstance(){
	TCHAR mutex_name [MAX_PATH] = {0};
	strcpy (mutex_name, _folderWatcher.getLockFile().c_str());

	// replace "\" by "_"
	for(TCHAR* ch = mutex_name; *ch != _T('\0'); ++ch)
		if(*ch == _T('\\')) *ch = _T('_');

	if(::OpenMutex(MUTEX_ALL_ACCESS, FALSE, mutex_name) != NULL) {
		log("a daemon is already running");
		return false;
	}

	if(::CreateMutex(NULL, TRUE, mutex_name) == NULL) {
		log("cannot create unicity mutex");
		return false;
	}

	return true;
}
/**
 * Messages received by the window
 * 	WM_DESTROY
 * 	WM_REMOVE_WATCH
 *
 */
LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
	switch (message) {
	case WM_DESTROY:
		PostQuitMessage(0);
		break;
	case WM_REMOVE_WATCH:
		_win32FSHook->remove_watch(lParam);
		log("watch %d removed", lParam);
		return 0;
	default:
		return DefWindowProc(hWnd, message, wParam, lParam);
	}
	return 0;
}

/**
 * Creates main window
 *
 * Necessary to be able to receive messages
 *
 */
bool InitInstance() {

// We don't need icon, since daemon is invisible
//	HICON hDocFetcherIcon = ::ExtractIcon(_hInstance, "DocFetcher.ico", 0);
//	if (hDocFetcherIcon == NULL) {
//		log("icon not found...");
//		hDocFetcherIcon = LoadIcon((HINSTANCE) NULL, IDI_APPLICATION);
//	}


	WNDCLASS wc;

	const char *CLASS_DOCFETCHER_WND = "DocFectcherDaemonWnd";

	wc.style = 0;
	wc.lpfnWndProc = (WNDPROC) WndProc;
	wc.cbClsExtra = 0;
 	wc.cbWndExtra = 0;
	wc.hInstance = _hInstance;
	wc.hIcon = NULL;
	wc.hCursor = LoadCursor((HINSTANCE) NULL, IDC_ARROW);
	wc.hbrBackground = NULL;
	wc.lpszMenuName = "MainMenu";
	wc.lpszClassName = CLASS_DOCFETCHER_WND;

	if (!RegisterClass(&wc))
		return false;

	// Create the main window.
	_hwndMain = CreateWindow(CLASS_DOCFETCHER_WND, "Sample",
			WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT,
			CW_USEDEFAULT, CW_USEDEFAULT, (HWND) NULL,
			(HMENU) NULL, _hInstance, (LPVOID) NULL);

	if (!_hwndMain) {
		return false;
	}

	return true;
}


