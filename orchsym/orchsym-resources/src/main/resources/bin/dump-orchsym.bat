@echo off
rem   
rem   Licensed to the Orchsym Runtime under one or more contributor license
rem   agreements. See the NOTICE file distributed with this work for additional
rem   information regarding copyright ownership.
rem   
rem   this file to You under the Orchsym License, Version 1.0 (the "License");
rem   you may not use this file except in compliance with the License.
rem   You may obtain a copy of the License at
rem   
rem   https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
rem   
rem   Unless required by applicable law or agreed to in writing, software
rem   distributed under the License is distributed on an "AS IS" BASIS,
rem   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem   See the License for the specific language governing permissions and
rem   limitations under the License.
rem   

rem Set environment variables

call %~sdp0\orchsym-env.bat

rem Use JAVA_HOME if it's set; otherwise, just use java

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
goto startNifi

:noJavaHome
echo The JAVA_HOME environment variable is not defined correctly.
echo Instead the PATH will be used to find the java executable.
echo.
set JAVA_EXE=java
goto startNifi

:startNifi
pushd "%NIFI_ROOT%"
set LIB_DIR=lib\bootstrap
set CONF_DIR=conf

set BOOTSTRAP_CONF_FILE=%CONF_DIR%\bootstrap.conf
set JAVA_ARGS=-Dorg.apache.nifi.bootstrap.config.log.dir=%NIFI_LOG_DIR% -Dorg.apache.nifi.bootstrap.config.pid.dir=%NIFI_PID_DIR% -Dorg.apache.nifi.bootstrap.config.file=%BOOTSTRAP_CONF_FILE%

SET JAVA_PARAMS=-cp %CONF_DIR%;%LIB_DIR%\* -Xms12m -Xmx24m %JAVA_ARGS% ${orchsym.boot.app}
set BOOTSTRAP_ACTION=dump

cmd.exe /C "%JAVA_EXE%" %JAVA_PARAMS% %BOOTSTRAP_ACTION%

popd
