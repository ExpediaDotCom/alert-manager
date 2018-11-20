
FROM openjdk:8-jre
MAINTAINER Adaptive Alerting <adaptive-alerting@expedia.com>

ENV APP_NAME alert-manager-store
ENV APP_HOME /app/bin
ENV JMXTRANS_AGENT jmxtrans-agent-1.2.6

RUN mkdir -p ${APP_HOME}
RUN mkdir -p ${APP_HOME}/storage-backends

COPY target/alert-manager-store-1.0.0-SNAPSHOT.jar ${APP_HOME}/alert-manager-store.jar
COPY storage-backends ${APP_HOME}/storage-backends
COPY docker/start-app.sh ${APP_HOME}/
COPY docker/jmxtrans-agent.xml ${APP_HOME}/

ADD https://github.com/jmxtrans/jmxtrans-agent/releases/download/${JMXTRANS_AGENT}/${JMXTRANS_AGENT}.jar ${APP_HOME}/

WORKDIR ${APP_HOME}

ENTRYPOINT ["./start-app.sh"]
