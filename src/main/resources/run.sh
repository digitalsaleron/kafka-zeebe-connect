#!/bin/sh

JAR_FILE=kafka-zeebe-connect.jar
CURRENT_DIR=$PWD

java -Dcom.sun.management.jmxremote=true \
-Dcom.sun.management.jmxremote.port=5000 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.local.only=false \
-Dlogging.config=$CURRENT_DIR/config/logback.xml \
-Djavax.net.ssl.trustStore=$CURRENT_DIR/config/truststore.jks \
-Djavax.net.ssl.trustStorePassword=changeit \
-jar $JAR_FILE