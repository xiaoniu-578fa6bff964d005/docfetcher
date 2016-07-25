How to raise the folder watch limit (Linux)
===========================================

On Linux, processes can by default watch at most 8192 folders. You might reach this limit if you index a very deep hierarchy of folders. If that happens, DocFetcher will probably show a "No space left on device" error message. You can work around this by raising the watch limit. For example, this command will temporarily raise the watch limit to 32000:

    sudo echo 32000 > /proc/sys/fs/inotify/max_user_watches

To change the watch limit permanently, open the file `/etc/sysctl.conf` (as root) and add the following line:

    fs.inotify.max_user_watches=32000
