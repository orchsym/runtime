#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"


xml_file=${RUNTIME_CONF_DIR}/state-management.xml
property_xpath='/stateManagement/cluster-provider/property'

edit_property() {
  property_name=$1
  property_value=$2

  if [ -n "${property_value}" ]; then
    xmlstarlet ed --inplace --update "${property_xpath}[@name='${property_name}']" --value "${property_value}" "${xml_file}"
  fi
}

edit_property 'Connect String'        "${RUNTIME_ZOOKEEPER_ENDPOINTS}"
edit_property 'Root Node'             "${RUNTIME_ZOOKEEPER_ROOT_NODE}"
edit_property 'Session Timeout'       "${RUNTIME_ZOOKEEPER_SESSION_TIMEOUT}"
