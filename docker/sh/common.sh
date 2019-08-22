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

export RUNTIME_HOME_DIR="/opt/orchsym/runtime"
export RUNTIME_CONF_DIR="${RUNTIME_HOME_DIR}/conf"
export RUNTIME_DATA_DIR="/data/runtime/data"
export RUNTIME_LOG_DIR="/data/runtime/log"
export RUNTIME_PID_DIR="/var/run/orchsym"

export RUNTIME_FLOWCONTROLLER_GRACEFUL_SHUTDOWN_PERIOD='10 sec'

# JVM
export RUNTIME_NODE_JVM_MEMORY='1024m'
export RUNTIME_JAVA_COMMAND='java'


# Logback logging levels and settings
export RUNTIME_LOG_APP_FILE_RETENTION=10
export RUNTIME_LOG_USER_FILE_RETENTION=10
export RUNTIME_LOG_BOOT_FILE_RETENTION=10


# Web properties
export RUNTIME_WEB_HTTP_HOST=${RUNTIME_WEB_HTTP_HOST:=$(hostname)}
export RUNTIME_WEB_HTTP_PORT=${RUNTIME_WEB_HTTP_PORT:=8080}
export RUNTIME_WEB_HTTPS_HOST=${RUNTIME_WEB_HTTPS_HOST:=$(hostname)}
export RUNTIME_WEB_HTTPS_PORT=${RUNTIME_WEB_HTTPS_PORT:=8443}
export RUNTIME_WEB_MAX_HEADER_SIZE=${RUNTIME_WEB_MAX_HEADER_SIZE:='16 KB'}
export RUNTIME_WEB_PROXY_CONTEXT_PATH=${RUNTIME_WEB_PROXY_CONTEXT_PATH:=''}
export RUNTIME_WEB_PROXY_HOST=${RUNTIME_WEB_PROXY_HOST:=''}


# NiFi cluster settings
export RUNTIME_SINGLE_NODE=${RUNTIME_SINGLE_NODE:=true}

# separated by ;
export RUNTIME_SERVERS=''
export RUNTIME_CLUSTER_NODE_ADDRESS=${RUNTIME_CLUSTER_NODE_ADDRESS:=''}
export RUNTIME_CLUSTER_NODE_PROTOCOL_PORT=${RUNTIME_CLUSTER_NODE_PROTOCOL_PORT:=9092}
# Site to Site properties
export RUNTIME_REMOTE_INPUT_HOST=${RUNTIME_REMOTE_INPUT_HOST:=''}
export RUNTIME_REMOTE_INPUT_SOCKET_PORT=${RUNTIME_REMOTE_INPUT_SOCKET_PORT:=9093}


# Logback logging levels and settings
export RUNTIME_LOG_APP_FILE_RETENTION=10
export RUNTIME_LOG_USER_FILE_RETENTION=10
export RUNTIME_LOG_BOOT_FILE_RETENTION=10

export RUNTIME_SSL_ENABLED='false'
export RUNTIME_AUTH_TYPE=${RUNTIME_AUTH_TYPE:=''}

# Security settings
# These default RUNTIME_initial_admin,keystorePasswd,keyPassed,trustsorePasswd string here
# is used together with the default preset keystore.jks,trustore.jks under files directory
# When NIFI is secure, all clients accessing NIFI MUST provide the client certificate and
export RUNTIME_CERTS_DIR=${RUNTIME_DATA_DIR}/certs
export RUNTIME_INITIAL_ADMIN='CN=Runtime, OU=Orchsym'
export RUNTIME_CLIENT_CERT_PASSWORD='orchsym'
export RUNTIME_SECURITY_KEYSTORE=${RUNTIME_CONF_DIR}/keystore.jks
export RUNTIME_SECURITY_KEYSTORETYPE=jks
export RUNTIME_SECURITY_KEYSTOREPASSWD='orchsym'
export RUNTIME_SECURITY_KEYPASSWD="orchsym"
export RUNTIME_SECURITY_TRUSTSTORE=${RUNTIME_CONF_DIR}/truststore.jks
export RUNTIME_SECURITY_TRUSTSTORETYPE=jks
export RUNTIME_SECURITY_TRUSTSTOREPASSWD='orchsym'

export RUNTIME_NEED_CLIENT_AUTH=${RUNTIME_NEED_CLIENT_AUTH:=''}

# Queue swap settings
export RUNTIME_QUEUE_SWAP_THRESHOLD=${RUNTIME_QUEUE_SWAP_THRESHOLD:=20000}
export RUNTIME_SWAP_IN_THREADS=${RUNTIME_SWAP_IN_THREADS:=1}
export RUNTIME_SWAP_OUT_THREADS=${RUNTIME_SWAP_OUT_THREADS:=4}


# Content Repository Settings
export RUNTIME_CONTENT_CLAIM_MAX_FLOW_FILES=100
export RUNTIME_CONTENT_CLAIM_MAX_APPENDABLE_SIZE='10 MB'
export RUNTIME_CONTENT_ARCHIVE_MAX_RETENTION_PERIOD='12 hours'
export RUNTIME_CONTENT_ARCHIVE_MAX_USAGE_PERCENTAGE='50%'
export RUNTIME_CONTENT_ARCHIVE_ENABLED='false'
export RUNTIME_CONTENT_ALWAYS_SYNC='false'

export RUNTIME_RUNTIME_FLOWCONTROLLER_GRACEFUL_SHUTDOWN_PERIOD=${RUNTIME_RUNTIME_FLOWCONTROLLER_GRACEFUL_SHUTDOWN_PERIOD:='10 sec'}


# Provenance Settings
export RUNTIME_PROVENANCE_IMPLEMENTATION=PersistentProvenanceRepository
export RUNTIME_PROVENANCE_MAX_STORAGE_TIME='24 hours'
export RUNTIME_PROVENANCE_MAX_STORAGE_SIZE='1 GB'
export RUNTIME_PROVENANCE_ROLLOVER_TIME='30 secs'
export RUNTIME_PROVENANCE_ROLLOVER_SIZE='100 MB'
export RUNTIME_PROVENANCE_QUERY_THREADS=2
export RUNTIME_PROVENANCE_INDEX_THREADS=2
export RUNTIME_PROVENANCE_REPOSITORY_BUFFER_SIZE=100000
export RUNTIME_PROVENANCE_INDEXED_FIELDS='EventType, FlowFileUUID, Filename, ProcessorID, Relationship'


# Status repository settings
export RUNTIME_COMPONENTS_STATUS_REPOSITORY_BUFFER_SIZE=1440
export RUNTIME_COMPONENTS_STATUS_SNAPSHOT_FREQUENCY='1 min'


# NiFi zookeeper settings
export RUNTIME_STATE_MANAGEMENT_EMBEDDED_ZOOKEEPER_START=false
export RUNTIME_ZOOKEEPER_SERVERS=''
export RUNTIME_ZOOKEEPER_DIR=/data/zookeeper
export RUNTIME_ZOOKEEPER_ROOT_NODE='/orchsym'
export RUNTIME_ZOOKEEPER_SESSION_TIMEOUT='10 seconds'
export RUNTIME_ZOOKEEPER_AUTOPURGE_PURGEINTERVAL=24
export RUNTIME_ZOOKEEPER_AUTOPURGE_SNAPRETAINCOUNT=30


export studio_props_file=${RUNTIME_CONF_DIR}/orchsym.properties

# 1 - value to search for
# 2 - value to replace
# 3 - file to perform replacement inline
function prop_replace () {
  target_file=${3:-${studio_props_file}}
  sed -i -e "s|^$1=.*$|$1=$2|"  ${target_file}
}

export -f prop_replace
