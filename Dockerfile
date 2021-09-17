FROM openjdk:8u292-jre

COPY target/kafka-zeebe-connect/ /opt/kafka-zeebe-connect/
WORKDIR /opt/kafka-zeebe-connect/
ENTRYPOINT ["/bin/sh", "run.sh"]