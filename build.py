#!/usr/bin/python

'''
This is not the actual build script. The real "build script" is a small Java
program in the package 'net.sourceforge.docfetcher.build'. This script merely
compiles and launches the Java builder.
'''

import os, shutil
from os.path import exists, join, isfile, isdir

print 'Cleaning build directory...'
if not exists('build'):
	os.makedirs('build')
for filename in os.listdir('build'):
	path = join('build', filename)
	if isfile(path):
		os.remove(path)
	elif isdir(path):
		shutil.rmtree(path)

package = 'net.sourceforge.docfetcher'
package_path = package.replace('.', '/')

print 'Copying sources to build directory...'
shutil.copytree(
	join('src', package_path),
	join('build/tmp/src-builder', package_path),
	ignore = shutil.ignore_patterns('.svn', '.cvs')
)

def execute(cmd_parts):
	os.system(' '.join(cmd_parts))

# Recursively collect library jars
jars = []
for root, dirs, files in os.walk('lib'):
	for filename in files:
		if not filename.endswith('.jar'): continue
		jars.append(join(root, filename))

print 'Compiling sources...'
execute([
	'javac',
	'-sourcepath build/tmp/src-builder',
	'-classpath \"%s\"' % ':'.join(jars),
	'-nowarn',
	join('build/tmp/src-builder', package_path, 'build/BuildMain.java')
])

jar_path = 'build/tmp/docfetcher-builder.jar'
main_class = package + '.build.BuildMain'

print 'Creating builder jar...'
execute([
	'jar cfe',
	jar_path,
	main_class,
	'-C build/tmp/src-builder net'
])

print 'Launching builder...'
print '-' * 40
jars.append(jar_path)
execute([
	'java',
	'-classpath \"%s\"' % ':'.join(jars),
	main_class
])
