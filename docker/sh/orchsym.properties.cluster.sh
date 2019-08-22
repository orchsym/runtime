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


# cluster node properties (only configure for cluster nodes) #
if [[ $RUNTIME_SINGLE_NODE == 'true' ]]; then
  prop_replace 'orchsym.cluster.is.node' false
else
  prop_replace 'orchsym.cluster.is.node' true
fi

# cluster common properties (all nodes must have same values) #
prop_replace 'orchsym.cluster.protocol.heartbeat.interval' "5 sec"
prop_replace 'orchsym.cluster.protocol.is.secure' ${RUNTIME_SSL_ENABLED}

prop_replace 'orchsym.cluster.node.address' "${RUNTIME_CLUSTER_NODE_ADDRESS}"
prop_replace 'orchsym.cluster.node.protocol.port' "${RUNTIME_CLUSTER_NODE_PROTOCOL_PORT}"
prop_replace 'orchsym.cluster.flow.election.max.wait.time' "2 mins"

# zookeeper properties, used for cluster management #
prop_replace 'orchsym.zookeeper.connect.string' "${RUNTIME_ZOOKEEPER_SERVERS}"
prop_replace 'orchsym.zookeeper.root.node' "${RUNTIME_ZOOKEEPER_ROOT_NODE}"

# external properties files for variable registry
# supports a comma delimited list of file locations
prop_replace 'orchsym.variable.registry.properties' "${RUNTIME_VARIABLE_REGISTRY_PROPERTIES}"
