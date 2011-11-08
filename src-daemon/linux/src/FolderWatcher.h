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

#ifndef FOLDERWATCHER_H_
#define FOLDERWATCHER_H_

struct WatchedFolder {
	std::string _path;
	bool _modified;
};

class FolderWatcher {
public:
	typedef std::map<int,WatchedFolder> folders_container_type;
	typedef std::map<int,int> 			inotify2id_container_type;

	FolderWatcher();
	virtual ~FolderWatcher();

	bool findIndexesFile();

	std::string getLockFile();

	bool startWatch();
	bool stopWatch();

	void run();


private:
	bool updateIndexesFile();
	bool addWatchRecursive(const bool, const int, const std::string &, const long);



	void callback(int watchID, int action);

	std::string _indexes_file_path;
	const char CHAR_MODIFIED;


	folders_container_type _indexed_folders;
	inotify2id_container_type   _inotify2id;

	int _fd;
};

#endif /*FOLDERWATCHER_H_*/
