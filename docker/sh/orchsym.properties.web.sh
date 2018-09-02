#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"


# Site to Site properties
prop_replace 'orchsym.remote.input.host' ${STUDIO_REMOTE_INPUT_HOST}
prop_replace 'orchsym.remote.input.secure' ${STUDIO_IS_SECURE}
prop_replace 'orchsym.remote.input.socket.port' ${STUDIO_REMOTE_INPUT_SOCKET_PORT}


# Web properties
if [[ $STUDIO_IS_SECURE == 'true' ]]; then
  prop_replace 'orchsym.web.http.host' ''
  prop_replace 'orchsym.web.http.port' ''
  prop_replace 'orchsym.web.http.network.interface.default' ''
  prop_replace 'orchsym.web.https.host' ${STUDIO_WEB_HTTPS_HOST}
  prop_replace 'orchsym.web.https.port' ${STUDIO_WEB_HTTPS_PORT}
  prop_replace 'orchsym.web.https.network.interface.default' ''
else
  prop_replace 'orchsym.web.http.host' ${STUDIO_WEB_HTTP_HOST}
  prop_replace 'orchsym.web.http.port' ${STUDIO_WEB_HTTP_PORT}
  prop_replace 'orchsym.web.http.network.interface.default' ''
  prop_replace 'orchsym.web.https.host' ''
  prop_replace 'orchsym.web.https.port' ''
  prop_replace 'orchsym.web.https.network.interface.default' ''
fi
prop_replace 'orchsym.web.max.header.size' "${STUDIO_WEB_MAX_HEADER_SIZE}"
prop_replace 'orchsym.web.proxy.context.path' ${STUDIO_WEB_PROXY_CONTEXT_PATH}
prop_replace 'orchsym.web.proxy.host' ${STUDIO_WEB_PROXY_HOST}


# if [ ! -z "${STUDIO_WEB_PROXY_HOST}" ]; then
#   echo 'STUDIO_WEB_PROXY_HOST was set but NiFi is not configured to run in a secure mode.  Will not update orchsym.web.proxy.host.'
# fi
