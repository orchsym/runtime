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



rem The java implementation to use
rem set JAVA_HOME="C:\Program Files\Java\jdk1.8.0"

set NIFI_ROOT=%~sdp0..\

rem The directory for the NiFi pid file
set NIFI_PID_DIR=%NIFI_ROOT%\run

rem The directory for NiFi log files
set NIFI_LOG_DIR=%NIFI_ROOT%\logs