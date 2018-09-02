FROM ocr.baishancloud.com/orchsym/openjdk:1.8.0

ARG PROJECT_NAME=runtime
ARG VERSION_NAME="1.7.0-SNAPSHOT"

ENV \
  STUDIO_BINARY_FILE=${PROJECT_NAME}-${VERSION_NAME}.tar.gz \
  STUDIO_HOME_DIR=/opt/orchsym/${PROJECT_NAME} \
  STUDIO_LOG_DIR=/data/${PROJECT_NAME}/log \
  STUDIO_DATA_DIR=/data/${PROJECT_NAME}/data

RUN yum install -y xmlstarlet && yum clean all

ADD nifi-assembly/target/${STUDIO_BINARY_FILE} /opt/orchsym/
ADD docker/sh/ ${STUDIO_HOME_DIR}/scripts/

RUN \
  mkdir -p ${STUDIO_LOG_DIR}/logs/ && \
  chmod +x ${STUDIO_HOME_DIR}/scripts/*.sh

WORKDIR ${STUDIO_HOME_DIR}

# Apply configuration and start NiFi
ENTRYPOINT bash ${STUDIO_HOME_DIR}/scripts/start.sh
