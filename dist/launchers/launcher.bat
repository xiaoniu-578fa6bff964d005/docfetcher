@echo off

rem  Note: This file is an alternative launcher for DocFetcher. How to use it:
rem  1) Modify this file as needed.
rem  2) Move this file one level up into the DocFetcher folder.
rem  3) Double-click on this file to launch DocFetcher.
rem  
rem  Common modifications:
rem  1) Replace the "java" keyword in the last line with the full path to the
rem  Java executable.
rem  2) Give DocFetcher more memory with the setting -Xmx..m in the last line.
rem  For example, with -Xmx512m, DocFetcher will use up to 512 MB of memory.
rem  Using more memory than about 1 GB requires a 64-bit Java runtime.

cd %~dp0

set libclasspath=

for %%f in (.\lib\*.jar) do (call :append_classpath %%f)
goto :proceed

:append_classpath
set libclasspath=%libclasspath%;%1
goto :eof

:proceed
java -enableassertions -Xmx512m -Xss2m -cp %libclasspath% -Djava.library.path=lib ${main_class} %1 %2 %3 %4 %5 %6 %7 %8 %9
