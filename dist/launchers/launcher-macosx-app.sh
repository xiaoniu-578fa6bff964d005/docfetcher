#!/bin/sh

scriptdir=$(cd "$(dirname "$0")"; pwd)
cd "$scriptdir"

CLASSPATH=
for FILE in `ls ../Resources/lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

# Note: The java call must not end with '&', otherwise the -Xdock:name property will have no effect.

/Library/Internet\ Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java -XstartOnFirstThread -Xdock:name="${app_name}" -enableassertions -Xmx512m -Xss2m -cp ".:${CLASSPATH}" -Djava.library.path="../Resources/lib" ${main_class} "$@"
