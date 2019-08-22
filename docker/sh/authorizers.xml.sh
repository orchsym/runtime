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


xml_file="${RUNTIME_CONF_DIR}/authorizers.xml"

runtime_servers_arr=(${RUNTIME_SERVERS//;/ })

xmlstarlet edit --inplace --update "/authorizers/userGroupProvider/property[@name='Users File']" --value "${RUNTIME_CONF_DIR}/users.xml" $xml_file
xmlstarlet edit --inplace --update "/authorizers/userGroupProvider/property[@name='Initial User Identity A']" --value "${RUNTIME_INITIAL_ADMIN}" $xml_file

for i in ${!runtime_servers_arr[@]}; do
  xmlstarlet edit --inplace --update "/authorizers/userGroupProvider/property[@name='Initial User Identity $i']" --value "CN=${runtime_servers_arr[$i]}, OU=Orchsym" $xml_file
done

xmlstarlet edit --inplace --update  "/authorizers/accessPolicyProvider/property[@name='Authorizations File']" --value "${RUNTIME_CONF_DIR}/authorizations.xml" $xml_file
xmlstarlet edit --inplace --update "/authorizers/accessPolicyProvider/property[@name='Initial User Identity A']" --value "${RUNTIME_INITIAL_ADMIN}" $xml_file

for i in ${!runtime_servers_arr[@]}; do
  xmlstarlet edit --inplace --update "/authorizers/accessPolicyProvider/property[@name='Node Identity $i']" --value "CN=${runtime_servers_arr[$i]}, OU=Orchsym" $xml_file
done
