#!/bin/sh
#   
#   Licensed to the Orchsym Runtime under one or more contributor license
#   agreements. See the NOTICE file distributed with this work for additional
#   information regarding copyright ownership.
#   
#   this file to You under the Orchsym License, Version 1.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#   
#   https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
#   
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#   

# The java implementation to use.
#export JAVA_HOME=/usr/java/jdk1.8.0/

export NIFI_HOME=$(cd "${SCRIPT_DIR}" && cd .. && pwd)

#The directory for the NiFi pid file
export NIFI_PID_DIR="${NIFI_HOME}/run"

#The directory for NiFi log files
export NIFI_LOG_DIR="${NIFI_HOME}/logs"

# Set to false to force the use of Keytab controller service in processors
# that use Kerberos. If true, these processors will allow configuration of keytab
# and principal directly within the processor. If false, these processors will be
# invalid if attempting to configure these properties. This may be advantageous in
# a multi-tenant environment where management of keytabs should be performed only by
# a user with elevated permissions (i.e., users that have been granted the 'ACCESS_KEYTAB'
# restriction).
export NIFI_ALLOW_EXPLICIT_KEYTAB=true