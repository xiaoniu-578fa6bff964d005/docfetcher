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


#include "Win32FSHook.h"
#include <stdio.h>
#include <windows.h>
#include <winbase.h>
#include <winnt.h>
#include <time.h>
#include <stdio.h>
#include "Lock.h"
#include "WatchData.h"
#include "Logger.h"
#include <process.h>

Win32FSHook::Win32FSHook()
{
	_isRunning = false;
	InitializeCriticalSection(&_cSection);
	_mainLoopEvent = CreateEvent(NULL, FALSE,FALSE, NULL);
}

void Win32FSHook::init(ChangeCallback callback)
{
	if (!_isRunning)
	{
		_isRunning = true;

	    LPVOID dwThrdParam = (LPVOID)this;

	    // _beginthread is safer than CreateThread
	    // see http://msdn.microsoft.com/en-us/library/ms682453.aspx

	    _mainLoopThreadHandle = (HANDLE)_beginthread(Win32FSHook::mainLoop, 0, dwThrdParam);

		if (_mainLoopThreadHandle == 0)
		{
			throw ERR_INIT_THREAD;
		}
	}
}

Win32FSHook::~Win32FSHook()
{
	debug("+Win32FSHook destructor");
	// terminate thread.
	_isRunning = false;
	SetEvent(_mainLoopEvent);

	// cleanup
	CloseHandle(_mainLoopThreadHandle);
	CloseHandle(_mainLoopEvent);
	DeleteCriticalSection(&_cSection);
	debug("-Win32FSHook destructor");
}

void Win32FSHook::remove_watch(int wd)
{
	debug("+remove_watch(%d)", wd);
	EnterCriticalSection(&_cSection);

	map <int, WatchData*>::const_iterator i = _wid2WatchData.find(wd);
	if (i == _wid2WatchData.end())
	{
		debug("remove_watch: watch id %d not found", wd);
		LeaveCriticalSection(&_cSection);
	}
	else
	{
		WatchData *watchData = i->second;
		_pendingActions.push(make_pair(CANCEL, wd));
		SetEvent(_mainLoopEvent);
		LeaveCriticalSection(&_cSection);
		watchData->waitForEvent();
	}

	debug("-remove_watch(%d)", wd);
}

int Win32FSHook::add_watch(const WCHAR* path, long notifyFilter, bool watchSubdirs, DWORD &error, ChangeCallback changeCallback)
{
	debug("+add_watch(%ls)", path);
	// synchronize access by multiple threads
	EnterCriticalSection(&_cSection);
	WatchData *watchData;

	try
	{
		watchData = new WatchData(path, notifyFilter, watchSubdirs, Win32FSHook::changeCallback, changeCallback);
	}
	catch (DWORD err)
	{
		error = err;
		LeaveCriticalSection(&_cSection);
		return 0;
	}

	int watchId = watchData->getId();
	_wid2WatchData[watchId] = watchData;
	_pendingActions.push(make_pair(WATCH, watchId));
	SetEvent(_mainLoopEvent);
	LeaveCriticalSection(&_cSection);

	watchData->waitForEvent();

	debug("-add_watch(%ls)", path);
	return watchId;
}

void CALLBACK Win32FSHook::changeCallback(DWORD dwErrorCode, DWORD dwNumberOfBytesTransfered,  LPOVERLAPPED lpOverlapped)
{
	debug("+changeCallback");
	int wd = (int)lpOverlapped->hEvent;

	map <int, WatchData*>::const_iterator it = _win32FSHook->_wid2WatchData.find(wd);
	if (it == _win32FSHook->_wid2WatchData.end())
	{
		debug("-changeCallback: watch id %d not found", wd);
		return;
	}
	WatchData *watchData = it->second;
	_win32FSHook->handlePendingActions();
	map <int, WatchData*>::const_iterator ii = _win32FSHook->_wid2WatchData.find(watchData->getId());
	if (ii == _win32FSHook->_wid2WatchData.end())
	{
		log("-changeCallback : ignoring event for watch id %d, no longer in wid2WatchData map", wd);
		return;
	}

	char* events=(char*)watchData->getNotifyInfo();
	FILE_NOTIFY_INFORMATION *event;
	int i=0;
	do
	{
		event = (FILE_NOTIFY_INFORMATION*)(events+i);
		int action = event->Action;
		int len = event->FileNameLength / sizeof(WCHAR);

		WCHAR *name = new WCHAR[len + 1];
		for (int k=0;k<len;k++)
		{
			name[k] = event->FileName[k];
		}
		name[len] = 0;

		// log("%ls : Event %d on %ls (len =%d)",wd->getPath(), action, name, len);
		watchData->getCallback()(watchData->getId(), action, watchData->getPath(), name);

		delete[] name;
		i = event->NextEntryOffset;
	}
	while (event->NextEntryOffset);

	int res = watchData->watchDirectory();
	if (res != 0)
	{
		log("Error watching dir %s : %d",watchData->getPath(), res);
	}

	debug("-changeCallback");
}

void Win32FSHook::mainLoop( LPVOID lpParam )
{
	debug("mainLoop starts");
	Win32FSHook* _this = (Win32FSHook*)lpParam;
	while (_this->_isRunning)
	{
		_this->handlePendingActions();

		if (_this->_isRunning)
		{
			WaitForSingleObjectEx(_this->_mainLoopEvent, INFINITE, TRUE);
		}
	}
	debug("mainLoop exits");
	return;
}


void Win32FSHook::unwatchDirectory(WatchData* wd)
{
	debug("Win32FSHook::unwatchDirectory(%d)",wd->getId());
	debug("Stop watching %ls", wd->getPath());
	int res = wd->unwatchDirectory();
	if (res != 0)
	{
		log("Error canceling watch on dir %ls : %d",wd->getPath(), res);
	}
	else
	{
		int res2 = _wid2WatchData.erase(wd->getId());
		if (res2 == 1)
		{
			delete wd;
		}
		else
		{
			log("Error deleting watch %d from map, res=%d",wd->getId(), res2);
		}
	}
}

void Win32FSHook::watchDirectory(WatchData* wd)
{
	debug("Watching %ls", wd->getPath());
	int res = wd->watchDirectory();
	if (res != 0)
	{
		log("Error watching dir %ls : %d",wd->getPath(), res);
	}
}

void Win32FSHook::handlePendingActions()
{
	debug("+Win32FSHook::handlePendingActions called");
	EnterCriticalSection(&_cSection);

	while (_isRunning && _pendingActions.size() > 0)
	{
		debug("Win32FSHook::iteration");
		pair<ACTION, int> action = _pendingActions.front();
		_pendingActions.pop();
		switch (action.first)
		{
			case WATCH:
			{
				debug("Win32FSHook::handlePendingActions WATCH");
				int wd = action.second;
				map <int, WatchData*>::const_iterator i = _wid2WatchData.find(wd);
				if (i == _wid2WatchData.end())
				{
					debug("WATCH: watch id %d not found", wd);
				}
				else
				{
					WatchData* watchData = i->second;
					watchDirectory(watchData);
					watchData->signalEvent();
				}
			}
			break;
			case CANCEL:
			{
				debug("Win32FSHook::handlePendingActions CANCEL");
				int wd = action.second;
				map <int, WatchData*>::const_iterator i = _wid2WatchData.find(wd);
				if (i == _wid2WatchData.end())
				{
					debug("CANCEL: watch id %d not found", wd);
				}
				else
				{
					debug("Win32FSHook::handlePendingActions - calling unwatch ptr=%d", i->second);
					WatchData* watchData = i->second;
					// unwatching the watchdata cause the watch signal object
					// to be triggered.
					unwatchDirectory(watchData);
				}
			}
			break;
		}
	}

	LeaveCriticalSection(&_cSection);
	debug("-Win32FSHook::handlePendingActions");
}

