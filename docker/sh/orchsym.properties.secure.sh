#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[ -f "${SCRIPT_DIR}/common.sh" ] && source "${SCRIPT_DIR}/common.sh"



# Perform idempotent changes of configuration to support secure environments
echo 'Configuring environment with SSL settings'

if [[ $STUDIO_IS_SECURE == 'true' ]]; then
  prop_replace 'orchsym.security.keystore' ${STUDIO_SECURITY_KEYSTORE}
  prop_replace 'orchsym.security.keystoreType' ${STUDIO_SECURITY_KEYSTORETYPE}
  prop_replace 'orchsym.security.keystorePasswd' ${STUDIO_SECURITY_KEYSTOREPASSWD}
  prop_replace 'orchsym.security.keyPasswd' ${STUDIO_SECURITY_KEYPASSWD}
  prop_replace 'orchsym.security.truststore' ${STUDIO_SECURITY_TRUSTSTORE}
  prop_replace 'orchsym.security.truststoreType' ${STUDIO_SECURITY_TRUSTSTORETYPE}
  prop_replace 'orchsym.security.truststorePasswd' ${STUDIO_SECURITY_TRUSTSTOREPASSWD}
else
  prop_replace 'orchsym.security.keystore' ''
  prop_replace 'orchsym.security.keystoreType' ''
  prop_replace 'orchsym.security.keystorePasswd' ''
  prop_replace 'orchsym.security.keyPasswd' ''
  prop_replace 'orchsym.security.truststore' ''
  prop_replace 'orchsym.security.truststoreType' ''
  prop_replace 'orchsym.security.truststorePasswd' ''
fi

prop_replace 'orchsym.security.needClientAuth' ${STUDIO_NEED_CLIENT_AUTH}
