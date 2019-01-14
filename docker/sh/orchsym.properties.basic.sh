#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"


prop_replace 'orchsym.flow.configuration.file' ${RUNTIME_DATA_DIR}/flow.xml.gz

prop_replace 'orchsym.authorizer.configuration.file' ${RUNTIME_CONF_DIR}/authorizers.xml
prop_replace 'orchsym.login.identity.provider.configuration.file' ${RUNTIME_CONF_DIR}/login-identity-providers.xml

## H2
prop_replace 'orchsym.database.directory' ${RUNTIME_DATA_DIR}/database_repository

## FlowFile Repository
prop_replace 'orchsym.flowfile.repository.directory' ${RUNTIME_DATA_DIR}/flowfile_repository
prop_replace 'orchsym.queue.swap.threshold' ${RUNTIME_QUEUE_SWAP_THRESHOLD}
prop_replace 'orchsym.swap.in.threads' ${RUNTIME_SWAP_IN_THREADS}
prop_replace 'orchsym.swap.out.threads' ${RUNTIME_SWAP_OUT_THREADS}


# Content Repository
prop_replace 'orchsym.content.repository.directory.default' ${RUNTIME_DATA_DIR}/content_repository
prop_replace 'orchsym.content.claim.max.appendable.size' "${RUNTIME_CONTENT_CLAIM_MAX_APPENDABLE_SIZE}"
prop_replace 'orchsym.content.claim.max.flow.files' ${RUNTIME_CONTENT_CLAIM_MAX_FLOW_FILES}
prop_replace 'orchsym.content.repository.archive.max.retention.period' "${RUNTIME_CONTENT_ARCHIVE_MAX_RETENTION_PERIOD}"
prop_replace 'orchsym.content.repository.archive.max.usage.percentage' ${RUNTIME_CONTENT_ARCHIVE_MAX_USAGE_PERCENTAGE}
prop_replace 'orchsym.content.repository.archive.enabled' ${RUNTIME_CONTENT_ARCHIVE_ENABLED=}
prop_replace 'orchsym.content.repository.always.sync' ${RUNTIME_CONTENT_ALWAYS_SYNC=}


# Provenance Repository Properties
prop_replace 'orchsym.provenance.repository.implementation' org.apache.nifi.provenance.${RUNTIME_PROVENANCE_IMPLEMENTATION}
prop_replace 'orchsym.provenance.repository.directory.default' ${RUNTIME_DATA_DIR}/provenance_repository
prop_replace 'orchsym.provenance.repository.max.storage.time' "${RUNTIME_PROVENANCE_MAX_STORAGE_TIME}"
prop_replace 'orchsym.provenance.repository.max.storage.size' "${RUNTIME_PROVENANCE_MAX_STORAGE_SIZE}"
prop_replace 'orchsym.provenance.repository.rollover.time' "${RUNTIME_PROVENANCE_ROLLOVER_TIME}"
prop_replace 'orchsym.provenance.repository.rollover.size' "${RUNTIME_PROVENANCE_ROLLOVER_SIZE}"
prop_replace 'orchsym.provenance.repository.query.threads' ${RUNTIME_PROVENANCE_QUERY_THREADS}
prop_replace 'orchsym.provenance.repository.index.threads' ${RUNTIME_PROVENANCE_INDEX_THREADS}
prop_replace 'orchsym.provenance.repository.indexed.fields' "${RUNTIME_PROVENANCE_INDEXED_FIELDS}"
prop_replace 'orchsym.provenance.repository.buffer.size' "${RUNTIME_PROVENANCE_REPOSITORY_BUFFER_SIZE}"

# Component Status Repository
prop_replace 'orchsym.components.status.repository.buffer.size' "${RUNTIME_COMPONENTS_STATUS_REPOSITORY_BUFFER_SIZE}"
prop_replace 'orchsym.components.status.snapshot.frequency' "${RUNTIME_COMPONENTS_STATUS_SNAPSHOT_FREQUENCY}"


prop_replace 'orchsym.flowcontroller.graceful.shutdown.period' "${RUNTIME_FLOWCONTROLLER_GRACEFUL_SHUTDOWN_PERIOD}"


## State Management
prop_replace 'orchsym.state.management.configuration.file' ${RUNTIME_CONF_DIR}/state-management.xml
prop_replace 'orchsym.state.management.embedded.zookeeper.properties' ${RUNTIME_CONF_DIR}/zookeeper.properties
# Specifies whether or not this instance should run an embedded ZooKeeper server
prop_replace 'orchsym.state.management.embedded.zookeeper.start' "${RUNTIME_STATE_MANAGEMENT_EMBEDDED_ZOOKEEPER_START:='false'}"
