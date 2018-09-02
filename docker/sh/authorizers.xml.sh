#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"


xml_file="${STUDIO_CONF_DIR}/authorizers.xml"

sudio_servers_arr=(${STUDIO_SERVERS//;/ })

xmlstarlet edit --inplace --update "/authorizers/userGroupProvider/property[@name='Users File']" --value "${STUDIO_CONF_DIR}/users.xml" $xml_file
xmlstarlet edit --inplace --update "/authorizers/userGroupProvider/property[@name='Initial User Identity A']" --value "${STUDIO_INITIAL_ADMIN}" $xml_file

for i in ${!studio_servers_arr[@]}; do
  xmlstarlet edit --inplace --update "/authorizers/userGroupProvider/property[@name='Initial User Identity $i']" --value "CN=${studio_servers_arr[$i]}, OU=Orchsym" $xml_file
done

xmlstarlet edit --inplace --update  "/authorizers/accessPolicyProvider/property[@name='Authorizations File']" --value "${STUDIO_CONF_DIR}/authorizations.xml" $xml_file
xmlstarlet edit --inplace --update "/authorizers/accessPolicyProvider/property[@name='Initial User Identity A']" --value "${STUDIO_INITIAL_ADMIN}" $xml_file

for i in ${!studio_servers_arr[@]}; do
  xmlstarlet edit --inplace --update "/authorizers/accessPolicyProvider/property[@name='Node Identity $i']" --value "CN=${studio_servers_arr[$i]}, OU=Orchsym" $xml_file
done
