/*******************************************************************************
 * JNotify - Allow java applications to register to File system events.
 *
 * Copyright (C) 2005 - Content Objects
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 ******************************************************************************
 *
 * Content Objects, Inc., hereby disclaims all copyright interest in the
 * library `JNotify' (a Java library for file system events).
 *
 * Yahali Sherman, 21 November 2005
 *    Content Objects, VP R&D.
 *
 ******************************************************************************
 * Author : Omry Yadan
 ******************************************************************************/




#ifndef WIN32FSHOOK_H_
#define WIN32FSHOOK_H_

#include <windows.h>
#include <string>
#include <map>
#include <queue>
#include <utility>

#include "WatchData.h"

using namespace std;

extern class Win32FSHook *_win32FSHook;

class Win32FSHook
{
private:
	enum ACTION {WATCH,CANCEL};

	// running flag
	bool _isRunning;

	// thread handle
	HANDLE _mainLoopThreadHandle;

	HANDLE _mainLoopEvent;

	// critical seaction
	CRITICAL_SECTION _cSection;

	// watch id 2 watch map
	map<int, WatchData*> _wid2WatchData;

	// a vector of pending actions.
	// each pair is the action and the watch id this action relates to.
	queue< pair<ACTION,int> > _pendingActions;

	// Thread function
	static void mainLoop( LPVOID lpParam );
	// completion routine.
	static void CALLBACK changeCallback(DWORD dwErrorCode, DWORD dwNumberOfBytesTransfered,  LPOVERLAPPED lpOverlapped);

	void watchDirectory(WatchData* wd);

	void unwatchDirectory(WatchData* wd);

	void handlePendingActions();
public:
	static const int ERR_INIT_THREAD = 1;

	Win32FSHook();
	virtual ~Win32FSHook();

	void init(ChangeCallback callback);

	int add_watch(const WCHAR* path, long notifyFilter, bool watchSubdirs, DWORD &error,ChangeCallback changeCallback);
	void remove_watch(int watchId);
};

#endif /*WIN32FSHOOK_H_*/
