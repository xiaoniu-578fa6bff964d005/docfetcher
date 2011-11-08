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

#include "Logger.h"

#include <stdio.h>
#include <stdarg.h>
bool dbg = false;
void debug(const char *format, ...)
{
	if (dbg)
	{
		static char sbuf[1024];

		va_list args;
		va_start(args, format);
		vsnprintf(sbuf, 1024, format, args);
		va_end(args);
		printf("daemon : %s\n",sbuf);
		fflush(stdout);

	}
}

void log(const char *format, ...)
{
	static char sbuf[1024];

	va_list args;
	va_start(args, format);
	vsnprintf(sbuf, 1024, format, args);
	va_end(args);
	printf("daemon : %s\n",sbuf);
	fflush(stdout);

}
