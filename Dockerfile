FROM ocr.baishancloud.com/orchsym/openjdk:1.8.0

ARG PROJECT_NAME=runtime-ce
ARG VERSION_NAME="1.7.1-SNAPSHOT"

ENV \
  RUNTIME_BINARY_FILE=${PROJECT_NAME}-${VERSION_NAME}.tar.gz \
  RUNTIME_HOME_DIR=/opt/orchsym/${PROJECT_NAME} \
  RUNTIME_LOG_DIR=/data/${PROJECT_NAME}/log \
  RUNTIME_DATA_DIR=/data/${PROJECT_NAME}/data

RUN yum install -y xmlstarlet && yum clean all

ADD orchsym/orchsym-assembly/target/${RUNTIME_BINARY_FILE} /opt/orchsym/
ADD docker/sh/ ${RUNTIME_HOME_DIR}/scripts/

RUN \
  mkdir -p ${RUNTIME_LOG_DIR} && \
  sed -i "/^export ORCHSYM_LOG_DIR=/c\export ORCHSYM_LOG_DIR=${RUNTIME_LOG_DIR}" ${RUNTIME_HOME_DIR}/bin/orchsym-env.sh && \
  sed -i "/^export NIFI_LOG_DIR=/c\export NIFI_LOG_DIR=${RUNTIME_LOG_DIR}" ${RUNTIME_HOME_DIR}/bin/orchsym-env.sh && \
  chmod +x ${RUNTIME_HOME_DIR}/scripts/*.sh

WORKDIR ${RUNTIME_HOME_DIR}

# Apply configuration and start Runtime
ENTRYPOINT bash ${RUNTIME_HOME_DIR}/scripts/start.sh
