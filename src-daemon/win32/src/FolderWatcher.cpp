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


#include "jnotify_win32/Win32FSHook.h"
#include "jnotify_win32/Logger.h"

#include <shlobj.h>
#include <fstream>

#include "FolderWatcher.h"

/**
 * RaiiLocker
 * Pattern : Ressource Acquisition Is Initialization
 * locks and unlocks the given lock
 * Stores a REFERENCE to the lock
 *
 */
class RaiiLocker {
public:
	RaiiLocker(Lock &lock):_lock(lock) {
		_lock.lock();
	}
	~RaiiLocker(){
		_lock.unlock();
	}
private:
	Lock &_lock;
};


extern Win32FSHook *_win32FSHook;

FolderWatcher *_this = NULL;

extern HWND _hwndMain;


/**
 * constructor
 *
 */
FolderWatcher::FolderWatcher():CHAR_MODIFIED('#') {
	_this = this;
}

/**
 * destructor
 *
 */
FolderWatcher::~FolderWatcher() {
	_this = NULL;
}

/**
 * Initialization, called at the beginning
 *
 * initializes the map of indexed folders and adds the watches
 *
 */
bool FolderWatcher::startWatch() {

	// read file
	std::string line;
	log("opennig file %s", _indexes_file_path.c_str());
	std::ifstream in (_indexes_file_path.c_str());

	if(!in){
		log("Cannot open index file (%s)", _indexes_file_path.c_str());
		return false;
	}

	WCHAR  file_name [MAX_PATH * MB_CUR_MAX];
	DWORD error;

	const long notifyFilter =
		FILE_NOTIFY_CHANGE_FILE_NAME         //  A file has been added, deleted, or renamed in this directory.
		| FILE_NOTIFY_CHANGE_DIR_NAME        //  A subdirectory has been created, removed, or renamed.
		| FILE_NOTIFY_CHANGE_NAME            //  This directory's name has changed.
//		| FILE_NOTIFY_CHANGE_ATTRIBUTES      //  The value of an attribute of this file, such as last access time, has changed.
		| FILE_NOTIFY_CHANGE_SIZE            //  This file's size has changed.
		| FILE_NOTIFY_CHANGE_LAST_WRITE      //  This file's last modification time has changed.
//		| FILE_NOTIFY_CHANGE_LAST_ACCESS     //  This file's last access time has changed.
//		| FILE_NOTIFY_CHANGE_CREATION        //  This file's creation time has changed.
//		| FILE_NOTIFY_CHANGE_EA              //  This file's extended attributes have been modified.
//		| FILE_NOTIFY_CHANGE_SECURITY        //  This file's security information has changed.
//		| FILE_NOTIFY_CHANGE_STREAM_NAME     //  A file stream has been added, deleted, or renamed in this directory.
//		| FILE_NOTIFY_CHANGE_STREAM_SIZE     //  This file stream's size has changed.
//		| FILE_NOTIFY_CHANGE_STREAM_WRITE    //  This file stream's data has changed.
	;

	const bool watchSubdirs = true;

	WatchedFolder aWatchedFolder;
	aWatchedFolder._modified = false;

	log("locking");
	RaiiLocker aLock(_lock);

	while(std::getline(in,line)){
		log("line=%s", line.c_str());
		if(line.empty()){
			continue;
		}else if(line.at(0) == CHAR_MODIFIED){
			log("folder already signaled modified : %s", line.c_str());
			continue;
		}else if(line.size()>=2 && line.substr(0,2) == "//"){
			// a comment line, ignore
			continue;
		}else{
			log("add_watch for dir=%s", line.c_str());

			int count_chars = mbstowcs( NULL, line.c_str(), 0);
			mbstowcs(file_name, line.c_str(), count_chars);

			log("path converted to wide string");

			// null terminated string
			file_name[count_chars] = 0;

			try {
				log("calling add_watch");
				int watchId = _win32FSHook->add_watch((const WCHAR *)file_name, notifyFilter, watchSubdirs, error, &callback);
				if(watchId == 0 ) {
					log("error add_watch for dir=%s err=%d", line.c_str(), error);
				}else{
					log("Watch installed for directory %s",line.c_str());
					aWatchedFolder._path = line;
					_indexed_folders.insert(std::make_pair(watchId, aWatchedFolder));
				}
			}catch(...){
				log("error add_watch : default catch");
			}

		}

	}

	return (_indexed_folders.size() > 0);
}


/**
 * Remove all watches
 *
 */
bool FolderWatcher::stopWatch() {
	folders_container_type::const_iterator itFolder;
	for(itFolder = _indexed_folders.begin() ; itFolder != _indexed_folders.end() ; ++itFolder) {
		::PostMessage(_hwndMain, WM_REMOVE_WATCH, 0, itFolder->first);
	}
	_indexed_folders.clear();
	return true;
}
/**
 * Watches' callback
 *
 * Removes the watch and updates the indexes file
 *
 */
void FolderWatcher::callback(int watchID, int action, const WCHAR* rootPath, const WCHAR* filePath) {
	RaiiLocker aLock(_this->_lock);
	log("callback : watchID=%d,action=%d,rootPath=%ls,filePath=%ls",watchID,action,rootPath,filePath);

	if(_this == NULL) {
		log("_this == NULL ???");
		return;
	}

	if(_this->_indexed_folders.find(watchID) == _this->_indexed_folders.end()) {
		log("id unknown ???");
		return;
	}

	// impossible to remove here, we tell the main thread to do it
	::PostMessage(_hwndMain, WM_REMOVE_WATCH, 0, watchID);

	if(_this->_indexed_folders[watchID]._modified == true){
		log("already done...");
		return;

	}

	_this->_indexed_folders[watchID]._modified = true;

	if(!_this->updateIndexesFile()){
		log("updateIndexesFile failed");
		return;
	}

}

std::string FolderWatcher::getLockFile() {
	return _indexes_file_path + ".lock";
}

bool FolderWatcher::findIndexesFile() {

	// Portable version -> the directory ./indexes exists
	TCHAR current_path [MAX_PATH] = {0};
	::GetModuleFileName(NULL, current_path, MAX_PATH);

	std::string indexes_directory = current_path;
	indexes_directory.erase(indexes_directory.rfind('\\'));
	indexes_directory += "\\indexes";

	HANDLE hFind;
	WIN32_FIND_DATA FindFileData;
	hFind = ::FindFirstFile(indexes_directory.c_str(), &FindFileData);
	if(hFind != INVALID_HANDLE_VALUE) {
		// Portable version
		::FindClose(hFind);
		_indexes_file_path = indexes_directory;
		_indexes_file_path += "\\.indexes.txt";
		log("Portable version : working with file %s", _indexes_file_path.c_str());
		return true;
	}else{
		// Normal version -> indexes.txt is in APPDATA\DocFetcher folder
		log("Directory %s does not exist -> installed version", indexes_directory.c_str());

		TCHAR szPath[MAX_PATH];

		if(SUCCEEDED(SHGetFolderPath(NULL, CSIDL_APPDATA, NULL, 0, szPath)))
		{
			_indexes_file_path = szPath;
			_indexes_file_path += "\\DocFetcher\\.indexes.txt";
			log("Normal version : working with file %s", _indexes_file_path.c_str());
			return true;
		}else{
			log("Cannot get APPDATA ???? ");
			return false;
		}
	}
}

/**
 * Writes the indexes file
 *
 * If all folders are modified, we exit the daemon
 *
 */
bool FolderWatcher::updateIndexesFile() {

	bool bAllFoldersModified = true;

	log("Writing into %s",_indexes_file_path.c_str());

	std::ofstream out(_indexes_file_path.c_str());
	out << "//Updated by daemon" << std::endl;

	folders_container_type::const_iterator itFolder;
	for(itFolder = _indexed_folders.begin() ; itFolder != _indexed_folders.end() ; ++itFolder) {
		if(itFolder->second._modified) {
			out << CHAR_MODIFIED;
		} else {
			bAllFoldersModified = false;
		}
		out << itFolder->second._path << std::endl;
	}

	if(bAllFoldersModified) {
		log("nothing to do");
//		::PostMessage(_hwndMain,WM_DESTROY,0,0);
	}

	return true;
}

