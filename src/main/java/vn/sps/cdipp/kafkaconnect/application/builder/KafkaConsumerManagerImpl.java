/*
 * Class: KafkaConsumerManagerImpl
 *
 * Created on Sep 6, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.sps.cdipp.kafkaconnect.application.builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.camunda.zeebe.client.ZeebeClient;
import vn.sps.cdipp.kafkaconnect.application.handler.ResponseMessageHandler;
import vn.sps.cdipp.kafkaconnect.infrastructure.persistence.JobRepository;
import vn.sps.cdipp.kafkaconnect.infrastructure.properties.KafkaTopicProperties;
import vn.sps.cdipp.kafkaconnect.infrastructure.properties.PollerProperties;
import vn.sps.cdipp.kafkaconnect.infrastructure.properties.Wrapper;
import vn.sps.cdipp.kafkaconnect.model.JobInfo;
import vn.sps.cdipp.kafkaconnect.model.event.ConsumerRecoveryEvent;

@Component("kafkaConsumerManager")
public class KafkaConsumerManagerImpl implements KafkaConsumerManagerJmx, KafkaConsumerManager, ApplicationListener<ConsumerRecoveryEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerManagerImpl.class);

    private final Map<String, MessageHandler> kafkaConsumers = new ConcurrentHashMap<>();

    @Autowired
    private KafkaTopicProperties consumerTopicProperties;

    @Autowired
    private KafkaTopicProperties producerTopicProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private PollerProperties pollerProperties;

    @Autowired
    private BindingService bindingService;

    @Autowired
    private Wrapper wrapper;

    @Autowired
    private AbstractBindingTargetFactory<? extends MessageChannel> targetFactory;

    @Override
    public boolean findAndAddConsumerIfAbsent(final String consumerName, final MessageHandler handler) {
        
        synchronized (consumerName.intern()) {
            MessageHandler previousHandler = this.kafkaConsumers.putIfAbsent(consumerName, handler);
            if (previousHandler != null && !previousHandler.equals(handler)) {

                this.removeConsumer(consumerName);
                this.kafkaConsumers.put(consumerName, handler);
                LOGGER.debug(
                    "Detect the change of the consumer configuration {}. So remove the consumer {} to re-initialize",
                    consumerName, consumerName);
                return false;
            } else {
                LOGGER.debug("Existing consumer {} found in cache", consumerName);
                return previousHandler != null;
            }
        }
    }

    @Override
    public String removeConsumer(final String consumerName) {
        final String bindingName = new StringBuilder().append(consumerName).append("-in-0").toString();
        this.kafkaConsumers.remove(consumerName);
        this.bindingService.unbindConsumers(bindingName);
        return consumerName;
    }

    @Override
    public void onApplicationEvent(ConsumerRecoveryEvent event) {

        final JobRepository jobRepository = event.getJobRepository();
        final Map<String, JobInfo> jobInstances = event.getJobIntances();
        final String producerTopicSuffix = producerTopicProperties.getSuffix();
        final String consumerTopicSuffix = consumerTopicProperties.getSuffix();

        jobInstances.forEach((key, value) -> {
            final String jobElementId = value.getActivatedJob().getElementId();
            final String topicPrefix = this.detectTopicPrefix(jobElementId);
            final String topicName = new StringBuilder().append(topicPrefix).append(producerTopicSuffix).toString();
            final String consumerName = topicPrefix;
            final MessageHandler messageHandler = new ResponseMessageHandler(jobRepository, objectMapper, zeebeClient,
                pollerProperties.getCorrelationKey(), wrapper.getResponseWrapperKey());
            if (!findAndAddConsumerIfAbsent(consumerName, messageHandler)) {
                try {
                    KafkaConsumerBuilder.prepare(targetFactory, bindingService, messageHandler,
                        topicPrefix).setTopicSuffix(consumerTopicSuffix).build();
                    LOGGER.debug("Created consumer {} to consume topic {}", consumerName, topicName);
                } catch (Exception e) {
                    LOGGER.error("Error while building the consumer {}. Detail: ", topicPrefix, e);
                    this.removeConsumer(consumerName);
                    LOGGER.debug("Error while building the consumer. So remove the consumer {}", topicPrefix);
                }
            }
        });
        LOGGER.info("Completed recovery of consumers {}", kafkaConsumers.values());
    }

    private String detectTopicPrefix(String jobElementId) {
        if (consumerTopicProperties.isPrefixIsPattern()) {
            final String regex = consumerTopicProperties.getPrefix();
            final String string = jobElementId;

            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(string);
            matcher.find();
            return matcher.group(1);
        } else {
            return jobElementId;
        }
    }

    @Override
    public void reInitializeResponseConsumer(final String consumerName) {
        LOGGER.warn("This feature is not supported yet");
    }
}