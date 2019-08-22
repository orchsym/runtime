#!/bin/bash
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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"

crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' java ${RUNTIME_JAVA_COMMAND}
crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' lib.dir ./lib
crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' conf.dir ${RUNTIME_CONF_DIR}
crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' java.arg.2 "-Xms${RUNTIME_NODE_JVM_MEMORY}"
crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' java.arg.3 "-Xmx${RUNTIME_NODE_JVM_MEMORY}"
