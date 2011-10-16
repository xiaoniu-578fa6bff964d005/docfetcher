#!/bin/sh

scriptdir=$(cd "$(dirname "$0")"; pwd)
cd "$scriptdir"

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

java -XstartOnFirstThread -enableassertions -Xmx256m -cp ".:${CLASSPATH}" -Djava.library.path="lib" ${main_class} "$@" &