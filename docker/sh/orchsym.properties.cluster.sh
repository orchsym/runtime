#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"


# cluster node properties (only configure for cluster nodes) #
if [[ $STUDIO_SINGLE_NODE == 'true' ]]; then
  prop_replace 'orchsym.cluster.is.node' false
else
  prop_replace 'orchsym.cluster.is.node' true
fi

# cluster common properties (all nodes must have same values) #
prop_replace 'orchsym.cluster.protocol.heartbeat.interval' "5 sec"
prop_replace 'orchsym.cluster.protocol.is.secure' ${STUDIO_IS_SECURE}

prop_replace 'orchsym.cluster.node.address' "${STUDIO_CLUSTER_NODE_ADDRESS}"
prop_replace 'orchsym.cluster.node.protocol.port' "${STUDIO_CLUSTER_NODE_PROTOCOL_PORT}"
prop_replace 'orchsym.cluster.flow.election.max.wait.time' "2 mins"

# zookeeper properties, used for cluster management #
prop_replace 'orchsym.zookeeper.connect.string' "${STUDIO_ZOOKEEPER_SERVERS}"
prop_replace 'orchsym.zookeeper.root.node' "${STUDIO_ZOOKEEPER_ROOT_NODE}"

# external properties files for variable registry
# supports a comma delimited list of file locations
prop_replace 'orchsym.variable.registry.properties' "${STUDIO_VARIABLE_REGISTRY_PROPERTIES}"
