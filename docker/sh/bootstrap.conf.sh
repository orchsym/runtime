#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"

crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' java ${RUNTIME_JAVA_COMMAND}
crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' lib.dir ./lib
crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' conf.dir ${RUNTIME_CONF_DIR}
crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' java.arg.2 "-Xms${RUNTIME_NODE_JVM_MEMORY}"
crudini --set ${RUNTIME_CONF_DIR}/bootstrap.conf '' java.arg.3 "-Xmx${RUNTIME_NODE_JVM_MEMORY}"
