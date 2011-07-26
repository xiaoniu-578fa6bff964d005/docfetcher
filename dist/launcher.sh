#!/bin/sh

script=$(readlink -f "$0")
scriptdir=`dirname "$script"`
cd "$scriptdir"

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

if [ -d ./lib/linux ]; then
	for FILE in `ls ./lib/linux/*.jar`
	do
	   CLASSPATH=${CLASSPATH}:${FILE}
	done
fi

java -enableassertions -Xmx256m -cp ".:${CLASSPATH}" -Djava.library.path="lib" ${main_class} "$@"
