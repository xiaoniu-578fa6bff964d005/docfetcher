#!/bin/sh

# This script creates a disk image for Mac OS X on Linux.
#
# Requirements:
# - mkfs.hfsplus must be installed
# - the script build.py must be run first
# - this script must be run as root
#
# Output:
# - build/DocFetcher-{version-number}.dmg

if [ -z "$SUDO_COMMAND" ] # Need to run this with sudo
then
  mntusr=$(id -u) grpusr=$(id -g) sudo $0 $*
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

mkdir /mnt/tmp-docfetcher
mount -o loop $dmg_path /mnt/tmp-docfetcher
cp -r build/DocFetcher.app /mnt/tmp-docfetcher

umount /mnt/tmp-docfetcher
rm -rf /mnt/tmp-docfetcher
