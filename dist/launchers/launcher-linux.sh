#!/bin/sh

script=$(readlink -f "$0")
scriptdir=`dirname "$script"`
cd "$scriptdir"

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

if [ $(lsb_release -is) = "Ubuntu" ] && [ $XDG_CURRENT_DESKTOP = "Unity" ]; then
	export SWT_GTK3=0
fi

java -enableassertions -Xmx512m -Xss2m -cp ".:${CLASSPATH}" -Djava.library.path="lib" ${main_class} "$@"
