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




#include "Logger.h"

#include <stdio.h>
#include <windows.h>
#include "Lock.h"

Lock _logLoc;
bool dbg = false;
void debug(char *format, ...)
{
	if (dbg)
	{
		_logLoc.lock();
		static char sbuf[1024];
	
		va_list args;
		va_start(args, format);
		_vsnprintf(sbuf, 1024, format, args);
		va_end(args);
		printf("Win32 : %s\n",sbuf);
		fflush(stdout);
		
		_logLoc.unlock();
	}
}

void log(char *format, ...)
{
	_logLoc.lock();
	static char sbuf[1024];

	va_list args;
	va_start(args, format);
	_vsnprintf(sbuf, 1024, format, args);
	va_end(args);
	printf("Win32 : %s\n",sbuf);
	fflush(stdout);
	
	_logLoc.unlock();
}
