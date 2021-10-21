/*
 * Class: ProcessTriggerConfiguration
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
package vn.sps.cdipp.kafkaconnect.infrastructure.configuration;

import java.util.function.Consumer;

import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.camunda.zeebe.client.ZeebeClient;
import vn.sps.cdipp.kafkaconnect.application.builder.KafkaConsumerManager;
import vn.sps.cdipp.kafkaconnect.application.handler.DeploymentNotificationHandler;

@Configuration
public class ProcessTriggerConfiguration {

    private ObjectMapper objectMapper;

    private KafkaConsumerManager kafkaConsumerManager;

    private BindingService bindingService;

    private ZeebeClient zeebeClient;

    private AbstractBindingTargetFactory<? extends MessageChannel> targetFactory;

    public ProcessTriggerConfiguration(ObjectMapper objectMapper, KafkaConsumerManager kafkaConsumerManager,
            BindingService bindingService, ZeebeClient zeebeClient,
            AbstractBindingTargetFactory<? extends MessageChannel> targetFactory) {
        super();
        this.objectMapper = objectMapper;
        this.kafkaConsumerManager = kafkaConsumerManager;
        this.bindingService = bindingService;
        this.zeebeClient = zeebeClient;
        this.targetFactory = targetFactory;
    }

    @Bean
    public Consumer<JsonNode> deplopmentNotification() {
        return new DeploymentNotificationHandler(objectMapper, kafkaConsumerManager, bindingService, zeebeClient,
            targetFactory);
    }
}