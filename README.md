## Zeeb Kafka Connect
### Compatability
---

| Version | Kafka Version | Zeebe Version |
|---------------|---------------|-------------|
| 0.0.11 | 2.8 | 1.1.3

### Get Start
---

After build the source code, all the neccessary resource for running are copied into folder `kafka-zeebe-connect` under build directory. The folder includes:
* kafka-zeebe-connect.jar
* config
* run.sh

To quickly start the application, you should have a `kafka` and `zeebe` server avaiables at localhost. Then, you could trigger `run.sh` to start the connect application 

### Configuration
---

#### Kafka server
To configure kafka server, just change the array of kafka external endpoints into as following:
```yaml
spring:
  cloud:
    stream:
      binders:
        kafka1:
          type: kafka
          environment:
            spring:
              cloud:
                stream:
                  kafka:
                    binder:
                      brokers: kafka1:9092,kafka2:9092
                      configuration.security.protocol: PLAINTEXT
```
#### Zeebe gateway
To configure zeebe gateway, update following properties
```yaml
zeebe.client:
  requestTimeout: 10000
  broker.gatewayAddress: zeebe:26500
  security.plaintext: true
```
To configure `job type` and `correlationKey` that polled from Zeebe server, update following properties
```yaml
zeebe.client:
  poller:
    correlationKey: eventId
    jobType: ticketProcessing
```
#### Logging
#### Environment variables

| Variable Name | Default Value | Description |
|---------------|---------------|-------------|
| KAFKA_SECURITY_PROTOCOL | PLAINTEXT | Allowed value `SSL` or `PLAINTEXT`, which indicate the protocol for the connection with Kafka |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 | The external endpoint to access to Kafka server, this could be an array of one or more kafka servers separated by a comma
| ZEEBE_BROKER_GATEWAY | localhost:26500 | The endpoint of the Zeebe server gateway |
| ZEEBE_BROKER_SECURITY_PLAINTEXT_ENABLED | false | Accept value of `true` or `false` to indicate the SSL status of the gateway

