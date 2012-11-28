#!/bin/sh

# This script creates a disk image for Mac OS X on Linux.
#
# Requirements:
# - mkfs.hfsplus must be installed
# - the script build.py must be run first
# - do not run this as root, see bug #412
#
# Output:
# - build/DocFetcher-{version-number}.dmg

user=$(whoami)
if [ $user = "root" ]
then
	echo "Do not run this script as root."
	exit 0
fi

version=`cat current-version.txt`

du_output=`du -sk build/DocFetcher.app 2>&1`
dir_size=`echo $du_output | cut -f1 -d" "`
dir_size=`expr $dir_size + 1000`
dmg_path=build/DocFetcher-$version.dmg

rm -f $dmg_path
dd if=/dev/zero of=$dmg_path bs=1024 count=$dir_size
mkfs.hfsplus -v "DocFetcher" $dmg_path

sudo mkdir /mnt/tmp-docfetcher
sudo mount -o loop $dmg_path /mnt/tmp-docfetcher

sudo cp -r build/DocFetcher.app /mnt/tmp-docfetcher
sudo chown -R $user:$user /mnt/tmp-docfetcher

sudo umount /mnt/tmp-docfetcher
sudo rm -rf /mnt/tmp-docfetcher
