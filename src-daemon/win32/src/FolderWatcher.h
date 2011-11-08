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

#ifndef INDEXEDDIRECTORIES_H_
#define INDEXEDDIRECTORIES_H_

#include <map>
#include <string>
#include "jnotify_win32/Lock.h"


#define WM_REMOVE_WATCH       WM_USER + 1

struct WatchedFolder {
	std::string _path;
	bool _modified;
};

class FolderWatcher {
public:
	typedef std::map<int,WatchedFolder> folders_container_type;

	FolderWatcher();
	virtual ~FolderWatcher();

	bool findIndexesFile();

	std::string FolderWatcher::getLockFile();

	bool startWatch();
	bool stopWatch();

private:
	std::string _indexes_file_path;
	const char CHAR_MODIFIED;

	static void callback(int watchID, int action, const WCHAR* rootPath, const WCHAR* filePath);

	bool updateIndexesFile();

	folders_container_type _indexed_folders;

	Lock _lock;

};

#endif /* INDEXEDDIRECTORIES_H_ */
