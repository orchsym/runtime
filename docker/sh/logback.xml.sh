#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"

xml_file=${STUDIO_CONF_DIR}/logback.xml

xmlstarlet edit --inplace --update "/configuration/appender[@name='APP_FILE' and @class='ch.qos.logback.core.rolling.RollingFileAppender']/rollingPolicy/maxHistory" --value "${STUDIO_LOG_APP_FILE_RETENTION}" $xml_file
xmlstarlet edit --inplace --update "/configuration/appender[@name='USER_FILE' and @class='ch.qos.logback.core.rolling.RollingFileAppender']/rollingPolicy/maxHistory" --value "${STUDIO_LOG_USER_FILE_RETENTION}" $xml_file
xmlstarlet edit --inplace --update  "/configuration/appender[@name='BOOTSTRAP_FILE' and @class='ch.qos.logback.core.rolling.RollingFileAppender']/rollingPolicy/maxHistory" --value "${STUDIO_LOG_BOOT_FILE_RETENTION}" $xml_file
