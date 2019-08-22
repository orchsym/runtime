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
