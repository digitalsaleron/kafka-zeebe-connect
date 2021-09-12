FROM openjdk:8u292-jre

VOLUME /tmp
ADD target/kafka-zeebe-connect.jar /opt/kafka-zeebe-connect/kafka-zeebe-connect.jar
WORKDIR /opt/kafka-zeebe-connect
ENTRYPOINT ["java","-jar","kafka-zeebe-connect.jar"]