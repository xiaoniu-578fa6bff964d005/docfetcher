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



 
/**
 * This class is a auto-release lock.
 * there are two modes of usage.
 * 1. create with default constructor:
 * 		in this mode, the lock will create its own CRITICAL_SECTION, and you 
 * 		are responsible to call lock() and unlock() when you need to lock it.
 * 2. use the Lock(CRITICAL_SECTION *cSection, bool locked) contructor:
 * 		in this mode, you specify the critical section in construction, and 
 * 		a boolean flag that specify if the critical section is to be locked.
 */
 
#ifndef LOCK_H_
#define LOCK_H_

#include <windows.h>

class Lock
{
	CRITICAL_SECTION *_cSection;
public:
	Lock();
	Lock(CRITICAL_SECTION *cSection, bool locked);
	virtual ~Lock();
	void lock();
	void unlock();
};

#endif /*LOCK_H_*/
