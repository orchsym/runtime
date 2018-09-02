#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"

crudini --set ${STUDIO_CONF_DIR}/bootstrap.conf '' java ${STUDIO_JAVA_COMMAND}
crudini --set ${STUDIO_CONF_DIR}/bootstrap.conf '' lib.dir ./lib
crudini --set ${STUDIO_CONF_DIR}/bootstrap.conf '' conf.dir ${STUDIO_CONF_DIR}
crudini --set ${STUDIO_CONF_DIR}/bootstrap.conf '' java.arg.2 "-Xms${STUDIO_NODE_JVM_MEMORY}"
crudini --set ${STUDIO_CONF_DIR}/bootstrap.conf '' java.arg.3 "-Xmx${STUDIO_NODE_JVM_MEMORY}"
