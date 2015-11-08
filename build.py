#!/usr/bin/python

'''
This is not the actual build script. The real "build script" is a small Java
program in the package 'net.sourceforge.docfetcher.build'. This script merely
compiles and launches the Java builder.
'''

import os, shutil, platform
from os.path import exists, join, isfile, isdir

is_windows = 'windows' in platform.system().lower()
classpath_sep = ';' if is_windows else ':'

print('Cleaning build directory...')
if not exists('build'):
	os.makedirs('build')
for filename in os.listdir('build'):
	path = join('build', filename)
	if isfile(path):
		os.remove(path)
	elif isdir(path):
		shutil.rmtree(path)

print('Copying sources to build directory...')
shutil.copytree(
	'src',
	'build/tmp/src-builder',
	ignore = shutil.ignore_patterns('.svn', '.cvs', '.git')
)

def execute(cmd_parts):
	os.system(' '.join(cmd_parts))

# Recursively collect library jars
jars = []
for root, dirs, files in os.walk('lib'):
	for filename in files:
		if not filename.endswith('.jar'): continue
		jars.append(join(root, filename))

package = 'net.sourceforge.docfetcher'
package_path = package.replace('.', '/')

print('Compiling sources...')
compile_paths = [
	join('build/tmp/src-builder',\
	package_path, 'build/BuildMain.java')
]
for root, dirs, files in os.walk('build/tmp/src-builder'):
	for filename in files:
		if not filename.endswith('.java'):
			continue
		if filename.startswith('Test') or filename.endswith('Test.java'):
			path = join(root, filename)
			compile_paths.append(path)
execute([
	'javac',
	'-source 1.7',
	'-target 1.7',
	'-sourcepath build/tmp/src-builder',
	'-classpath \"%s\"' % classpath_sep.join(jars),
	'-nowarn',
	'-encoding', 'utf8', # Needed for some Tika source files
	' '.join(compile_paths)
])

jar_path = 'build/tmp/docfetcher-builder.jar'
main_class = package + '.build.BuildMain'

print('Creating builder jar...')
execute([
	'jar cfe',
	jar_path,
	main_class,
	'-C build/tmp/src-builder .'
])

print('Launching builder...')
print('-' * 40)
jars.append(jar_path)
execute([
	'java',
	'-enableassertions', # required by test classes
	'-classpath \"%s\"' % classpath_sep.join(jars),
	main_class
])
