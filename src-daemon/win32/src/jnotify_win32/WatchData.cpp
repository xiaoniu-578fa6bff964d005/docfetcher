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

 

#include "WatchData.h"
#include "Logger.h"

int WatchData::_counter = 0;

WatchData::WatchData()
{
}

WatchData::~WatchData()
{
	signalEvent();
	if (_path != NULL) free(_path);
	CloseHandle(_hDir);
	CloseHandle(_watchEventObject);
}

WatchData::WatchData(const WCHAR* path, int mask, bool watchSubtree, LPOVERLAPPED_COMPLETION_ROUTINE callback,ChangeCallback changeCallback)
	:
	_watchId(++_counter), 
	_mask(mask), 
	_watchSubtree(watchSubtree),
	_byteReturned(0),
	_callback(callback),
	_changeCallback(changeCallback)
{
	_path = _wcsdup(path); 
	_hDir = CreateFileW(_path,
						 FILE_LIST_DIRECTORY | GENERIC_READ | GENERIC_WRITE,
						 FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
						 NULL, //security attributes
						 OPEN_EXISTING,
						 FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OVERLAPPED, NULL);
	if(_hDir == INVALID_HANDLE_VALUE )	
	{
		throw GetLastError();
	}
	
	_overLapped.Internal = 0;
	_overLapped.InternalHigh = 0;
	_overLapped.Offset = 0;
	_overLapped.OffsetHigh = 0;
	_overLapped.hEvent = (HANDLE)_watchId;
	
	_watchEventObject = CreateEvent(NULL, FALSE,FALSE, NULL);
	
}

int WatchData::unwatchDirectory()
{
	if (CancelIo(_hDir) == FALSE)
	{
		return GetLastError();
	}
	else
	{
		return 0;
	}
}

int WatchData::watchDirectory()
{
	if( !ReadDirectoryChangesW( _hDir,
		 					    _notifyInfo,//<--FILE_NOTIFY_INFORMATION records are put into this buffer
								sizeof(_notifyInfo),
								_watchSubtree,
								_mask,
								&_byteReturned,
								&_overLapped,
								_callback))
	{
		return GetLastError();
	}
	else
	{
		return 0;
	}
}

void WatchData::waitForEvent()
{
	debug("+waitForEvent %d ", _watchId);
	WaitForSingleObjectEx(_watchEventObject, INFINITE, TRUE);
	debug("-waitForEvent %d ", _watchId);
}

void WatchData::signalEvent()
{
	SetEvent(_watchEventObject);
}
