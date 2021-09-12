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
package vn.ds.study.application.builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import io.camunda.zeebe.client.impl.ZeebeClientBuilderImpl;
import vn.ds.study.application.handler.ConsumerMessageHandler;
import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.infrastructure.properties.KafkaTopicProperties;
import vn.ds.study.infrastructure.properties.PollerProperties;
import vn.ds.study.model.JobInfo;
import vn.ds.study.model.event.ConsumerRecoveryEvent;

@Component("kafkaConsumerManager")
public class KafkaConsumerManagerImpl implements KafkaConsumerManager , ApplicationListener<ConsumerRecoveryEvent>{

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerManagerImpl.class);
    
    private final Map<String, String> kafkaConsumers = new ConcurrentHashMap<>();
    
    @Autowired
    private KafkaTopicProperties consumerTopicProperties;
    
    @Autowired
    private KafkaTopicProperties producerTopicProperties;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ZeebeClientBuilderImpl zeebeClientBuilder;

    @Autowired
    private PollerProperties pollerProperties;
    
    @Autowired
    private BindingService bindingService;
    
    @Autowired
    private AbstractBindingTargetFactory<? extends MessageChannel> targetFactory;
        
    @Override
    public boolean findAndAddConsumerIfAbsent(String consumerName) {
        final String previousValue = this.kafkaConsumers.putIfAbsent(consumerName, consumerName);
        return previousValue != null;
    }

    @Override
    public String removeConsumer(String consumerName) {
        return this.kafkaConsumers.remove(consumerName);
    }

    @Override
    public void onApplicationEvent(ConsumerRecoveryEvent event) {

        final JobRepository jobRepository = event.getJobRepository();
        final ZeebeClient zeebeClient = this.zeebeClientBuilder.build();
        final Map<String, JobInfo> jobInstances = event.getJobIntances();
        final String producerTopicSuffix = producerTopicProperties.getSuffix();
        final String consumerTopicSuffix = consumerTopicProperties.getSuffix();

        jobInstances.forEach((key, value) -> {

            final String topicPrefix = value.getActivatedJob().getElementId();
            final String topicName = new StringBuilder().append(topicPrefix).append(producerTopicSuffix).toString();
            final String consumerName = topicPrefix;
            if (!findAndAddConsumerIfAbsent(consumerName)) {
                try {
                    final MessageHandler messageHandler = new ConsumerMessageHandler(jobRepository, objectMapper,
                        zeebeClient, pollerProperties.getCorrelationKey());
                    KafkaConsumerBuilder.prepare(targetFactory, bindingService, messageHandler,
                        topicPrefix).setTopicSuffix(consumerTopicSuffix).build();
                    LOGGER.debug("Created consumer {} to consume topic {}", consumerName, topicName);
                } catch (Exception e) {
                    LOGGER.error("Error while building the consumer {}. Detail: ", topicPrefix, e);
                    this.removeConsumer(consumerName);
                    bindingService.unbindConsumers(consumerName);
                    LOGGER.debug("Error while building the consumer. So remove the consumer {}", topicPrefix);
                }
            }
        });
        LOGGER.info("Completed recovery of consumers {}", kafkaConsumers.values());
    }
}