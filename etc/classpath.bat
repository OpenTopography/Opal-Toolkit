@echo off
rem
rem This script set classpath
rem 	Usage: classpath [quiet]
rem By default it sets CLASSPATH to execute direct java invocations (all included)
rem by using quiet no echo of set CLASSOATH well be visible
rem

set LOCALCLASSPATH=
for %%i in (lib\*.jar) do call "%ANT_HOME%\bin\lcp.bat" %%i
for %%i in (webapps\opal2\WEB-INF\lib\*.jar) do call "%ANT_HOME%\bin\lcp.bat" %%i

set LOCALCLASSPATH=build\classes;%LOCALCLASSPATH%
set CLASSPATH=%LOCALCLASSPATH%

if "%1" == "quiet" goto end

echo %CLASSPATH%
goto end

:end
