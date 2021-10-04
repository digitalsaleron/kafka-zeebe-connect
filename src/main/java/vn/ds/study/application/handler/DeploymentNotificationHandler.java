/*
 * Class: DeploymentNotificationHandler
 *
 * Created on Sep 30, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.application.handler;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.impl.command.ArgumentUtil;
import vn.ds.study.application.builder.KafkaConsumerBuilder;
import vn.ds.study.application.builder.KafkaConsumerManager;
import vn.ds.study.model.DeploymentNotification;
import vn.ds.study.model.event.IntermediateEvent;
import vn.ds.study.model.event.MessageStartEvent;

public class DeploymentNotificationHandler implements Consumer<JsonNode>{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentNotificationHandler.class);

    private ObjectMapper objectMapper;
    
    private KafkaConsumerManager kafkaConsumerManager;
    
    private BindingService bindingService;
    
    private ZeebeClient zeebeClient;
    
    private AbstractBindingTargetFactory<? extends MessageChannel> targetFactory;

    public DeploymentNotificationHandler(ObjectMapper objectMapper, KafkaConsumerManager kafkaConsumerManager,
            BindingService bindingService, ZeebeClient zeebeClient,
            AbstractBindingTargetFactory<? extends MessageChannel> targetFactory) {
        super();
        this.objectMapper = objectMapper;
        this.kafkaConsumerManager = kafkaConsumerManager;
        this.bindingService = bindingService;
        this.zeebeClient = zeebeClient;
        this.targetFactory = targetFactory;
    }

    @Override
    public void accept(JsonNode jsonNode) {

        final DeploymentNotification notification = this.objectMapper.convertValue(jsonNode,
            DeploymentNotification.class);

        this.validateProcessId(notification.getProcessId());

        final List<MessageStartEvent> messageStartEvents = notification.getMessageStartEvents();
        messageStartEvents.forEach(event -> {
            this.createStartEventConsumer(event.getMessageName(), notification.getProcessId(),
                notification.getVersion());
        });
        final List<IntermediateEvent> intermediateEvents = notification.getIntermediateEvents();
        intermediateEvents.forEach(event -> {
            this.createIntermediateConsumer(event.getMessageName(), notification.getProcessId(),
                event.getCorrelationKey());
        });
        LOGGER.info("Receive a deployment notification from processId {} - version {}", notification.getProcessId(),
            notification.getVersion());
    }

    private void validateProcessId(String processId) {
        ArgumentUtil.ensureNotNullNorEmpty("processId", processId);
    }

    private void createStartEventConsumer(final String consumerName, final String processId, final Integer version) {
        final String topicName = consumerName;
        if (!kafkaConsumerManager.findAndAddConsumerIfAbsent(consumerName)) {
            try {
                final MessageHandler messageHandler = new StartMessageHandler(zeebeClient, objectMapper, processId, version);

                KafkaConsumerBuilder.prepare(targetFactory, bindingService, messageHandler,
                    topicName).setTopicSuffix("").build();

                LOGGER.info("Created start event consumer {} to consume topic {}", consumerName, topicName);
            } catch (Exception e) {
                LOGGER.error("Error while building the start event consumer {}. Detail: ", topicName, e);
                kafkaConsumerManager.removeConsumer(consumerName);
                bindingService.unbindConsumers(consumerName);
                LOGGER.debug("Error while building the start event consumer. So remove the consumer {}", topicName);
            }
        }
    }
    
    private void createIntermediateConsumer(final String consumerName, final String processId, final String correlationKey) {
        
        final String topicName = consumerName;
        final String messageName = consumerName;
        if (!kafkaConsumerManager.findAndAddConsumerIfAbsent(consumerName)) {
            try {
                final MessageHandler messageHandler = new IntermediateMessageHandler(objectMapper, zeebeClient,
                    processId, correlationKey, messageName);

                KafkaConsumerBuilder.prepare(targetFactory, bindingService, messageHandler, topicName).setTopicSuffix(
                    "").build();

                LOGGER.info("Created intermediate event consumer {} to consume topic {}", consumerName, topicName);
            } catch (Exception e) {
                LOGGER.error("Error while building the intermediate event consumer {}. Detail: ", topicName, e);
                kafkaConsumerManager.removeConsumer(consumerName);
                bindingService.unbindConsumers(consumerName);
                LOGGER.debug("Error while building the intermediate event consumer. So remove the consumer {}",
                    topicName);
            }
        }
    }
}