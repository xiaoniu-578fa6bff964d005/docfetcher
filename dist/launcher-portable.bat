@echo off

set libclasspath=

for %%f in (.\lib\*.jar) do (call :append_classpath %%f)
for %%f in (.\lib\windows\*.jar) do (call :append_classpath %%f)
goto :proceed

:append_classpath
set libclasspath=%libclasspath%;%1
goto :eof

:proceed
start /b javaw -enableassertions -Xmx256m -cp %libclasspath% -Djava.library.path=lib ${main_class} %1 %2 %3 %4 %5 %6 %7 %8 %9
