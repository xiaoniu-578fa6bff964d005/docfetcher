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


#include <sys/types.h>
#include <sys/time.h>
#include <sys/select.h>
#include <sys/ioctl.h>
//#include <linux/inotify.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>

#include <string>
#include <map>

#include <pthread.h>

#include <fstream>
#include <stdlib.h>

#include <fcntl.h>

#include "FolderWatcher.h"
#include "Logger.h"

extern bool dbg;

FolderWatcher _folderWatcher;


void *startThreadedWatch(void *threadid);
bool isUniqueInstance();


int main(){
	dbg = true;

	if(!_folderWatcher.findIndexesFile()) {
		log("findIndexesFile failed");
		return 1;
	}

	if(!isUniqueInstance()) {
		log("another instance is running...");
		return 1;
	}


	// Check of DocFetcher's lock file every 2 seconds
	// the command "lsof | grep .indexes.txt.lock$ >daemon.tmp"
	// tells if the file is used
	std::string lock_file = _folderWatcher.getLockFile();

	// the tmp file is put next to the lock file,
	// to be sure the daemon has write acces
	std::string tmp_file = _folderWatcher.getLockFile() + ".tmp";

	std::string cmd_line = "lsof | grep ";
	cmd_line += lock_file;
	cmd_line += "$ >";
	cmd_line += tmp_file;

	log("lock file : %s", lock_file.c_str());

	bool watching = false;
	for(;;){
		system(cmd_line.c_str());
		std::string line;
		std::ifstream in(tmp_file.c_str());
		std::getline(in, line);
		if(!line.empty()) {
//			log("lock file used");
			// the file is used by DocFetcher, so stop watching
			if(watching) {
				log("stopWatch");
				_folderWatcher.stopWatch();
				watching = false;
			}
		}else{
//			log("lock file not used");
			// the file is not used by DocFetcher, so start watching
			if(!watching) {
				log("startWatch");
				_folderWatcher.startWatch();
				watching = true;

				pthread_t thread;
				pthread_create(&thread, NULL, startThreadedWatch, NULL);
			}
		}

		// Check is done every 2 seconds
		sleep(2);
	}


	log("exit");

}

/**
 * Runs INotify in a different thread, to be able to stop it
 *
 */
void *startThreadedWatch(void *threadid) {
	_folderWatcher.run();
	return NULL;
}


/**
 * Check if this is the only instance
 * It is done by locking a file
 *
 */
bool isUniqueInstance(){
	struct flock fl;

	std::string daemon_lock_file = _folderWatcher.getLockFile();
	daemon_lock_file += ".daemon.lock";

	fl.l_type = F_WRLCK;
	fl.l_whence = SEEK_SET;
	fl.l_start = 0;
	fl.l_len = 1;
	int fdlock;
	if((fdlock = open(daemon_lock_file.c_str(), O_WRONLY|O_CREAT, 0666)) == -1) {
		log("cannot open daemon lock");
		return false;
	}

	if(fcntl(fdlock, F_SETLK, &fl) == -1) {
		log("daemon file locked : another instance is running");
		return false;

	}

	return true;

 }
