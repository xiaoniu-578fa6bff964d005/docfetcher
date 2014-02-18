#!/usr/bin/python

import urllib2, os, os.path as osp

'''
Description: This is a helper script for downloading the latest TrueZIP release
from http://search.maven.org. The downloaded files are stored under the current
directory.
'''

version = raw_input("Please enter TrueZIP version number (x.x.x): ")
root = "http://search.maven.org/remotecontent?filepath=de/schlichtherle/truezip"
out_dir = "truezip-" + version

modules = [
	"driver-file",
	"driver-tar",
	"driver-zip",
	"file",
	"kernel",
	"swing"
]

for module in modules:
	t_module = "truezip-" + module
	for suffix in ["", "-sources"]:
		filename = "%s-%s%s.jar" % (t_module, version, suffix)
		url = "/".join([root, t_module, version, filename])
		try:
			content = urllib2.urlopen(url)
		except urllib2.HTTPError:
			print("Download failed: " + url)
			exit(0)
		if not osp.exists(out_dir):
			os.makedirs(out_dir)
		path = osp.join(out_dir, filename)
		with open(path, 'w') as f:
			f.write(content.read())
		print("Downloaded: " + path)
