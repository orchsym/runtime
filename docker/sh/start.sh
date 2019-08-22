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


bash ${SCRIPT_DIR}/logback.xml.sh
bash ${SCRIPT_DIR}/bootstrap.conf.sh

bash ${SCRIPT_DIR}/orchsym.properties.basic.sh
bash ${SCRIPT_DIR}/orchsym.properties.cluster.sh
bash ${SCRIPT_DIR}/orchsym.properties.secure.sh
bash ${SCRIPT_DIR}/orchsym.properties.web.sh

# cluster mode
if [[ $RUNTIME_SINGLE_NODE == 'false' ]]; then
    bash ${SCRIPT_DIR}/state-management.sh
    bash ${SCRIPT_DIR}/zookeeper.properties.sh

fi


# secure mode
if [[ $RUNTIME_SSL_ENABLED == 'true' ]]; then
    bash ${SCRIPT_DIR}/authorizers.xml.sh

    # auth type only take effects when RUNTIME_SSL_ENABLED set to true
    case ${RUNTIME_AUTH_TYPE} in
        tls)
            # echo 'Enabling Two-Way SSL user authentication'
            # . "${SCRIPT_DIR}/secure.sh"
            :
            ;;
        ldap)
            # echo 'Enabling LDAP user authentication'
            # . "${SCRIPT_DIR}/secure.sh"
            # . "${SCRIPT_DIR}/update_login_providers.sh"
            :
            ;;
        *)
            :
            ;;
    esac
fi


# Continuously provide logs so that 'docker logs' can    produce them
# tail -F "${NIFI_HOME}/logs/nifi-app.log" &

# "${NIFI_HOME}/bin/nifi.sh" run &
# nifi_pid="$!"

# trap "echo Received trapped signal, beginning shutdown...;" KILL TERM HUP INT EXIT;

# echo NiFi running with PID ${nifi_pid}.
# wait ${nifi_pid}
exec "${RUNTIME_HOME_DIR}/bin/orchsym.sh" run
