#!/bin/sh

scriptdir=$(cd "$(dirname "$0")"; pwd)
cd "$scriptdir"
cd ../../..

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

# Note: The java call must not end with '&', otherwise the -Xdock:name property will have no effect.

java -XstartOnFirstThread -Xdock:name="${app_name}" -enableassertions -Xmx256m -cp ".:${CLASSPATH}" -Djava.library.path="lib" ${main_class} "$@"