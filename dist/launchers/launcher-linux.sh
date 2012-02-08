#!/bin/sh

script=$(readlink -f "$0")
scriptdir=`dirname "$script"`
cd "$scriptdir"

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

java -enableassertions -Xmx256m -Xss2m -cp ".:${CLASSPATH}" -Djava.library.path="lib" ${main_class} "$@"
