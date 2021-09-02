/*
 * Class: PollerConfiguration
 *
 * Created on Sep 1, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.infrastructure.configuration;

import java.time.Duration;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1.JobWorkerBuilderStep3;
import io.camunda.zeebe.client.impl.command.ArgumentUtil;
import io.camunda.zeebe.spring.client.properties.ZeebeClientConfigurationProperties;
import vn.ds.study.application.builder.ConsumerBuilder;
import vn.ds.study.application.handler.ConsumerMessageHandler;
import vn.ds.study.infrastructure.persistence.ConsumerRepository;
import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.infrastructure.properties.PollerProperties;
import vn.ds.study.model.JobInfo;

@Configuration
@EnableConfigurationProperties(value = { PollerProperties.class, KafkaBinderConfigurationProperties.class})
public class PollerConfiguration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PollerConfiguration.class);

    private ZeebeClientConfigurationProperties zeebeClientProperties;

    private PollerProperties pollerProperties;

    private JobRepository jobRepository;

    private ConsumerRepository consumerRepository;
    
    private ZeebeClient zeebeClient;
    
    private ZeebeClientBuilder zeebeClientBuilder;
    
    private StreamBridge streamBridge;
    
    private KafkaBinderConfigurationProperties kafkaBinderProperties;
    
    private ObjectMapper objectMapper;
    
    private AbstractBindingTargetFactory<? extends MessageChannel> targetFactory;
    
    private BindingService bindingService;

    public PollerConfiguration(ZeebeClientConfigurationProperties zeebeClientProperties,
            PollerProperties pollerProperties, JobRepository jobRepository, ConsumerRepository consumerRepository,
            ZeebeClientBuilder zeebeClientBuilder, StreamBridge streamBridge,
            KafkaBinderConfigurationProperties kafkaBinderProperties, ObjectMapper objectMapper,
            AbstractBindingTargetFactory<? extends MessageChannel> targetFactory, BindingService bindingService) {
        super();
        this.zeebeClientProperties = zeebeClientProperties;
        this.pollerProperties = pollerProperties;
        this.jobRepository = jobRepository;
        this.consumerRepository = consumerRepository;
        this.zeebeClientBuilder = zeebeClientBuilder;
        this.streamBridge = streamBridge;
        this.kafkaBinderProperties = kafkaBinderProperties;
        this.objectMapper = objectMapper;
        this.targetFactory = targetFactory;
        this.bindingService = bindingService;
        this.zeebeClient = this.zeebeClientBuilder.build();
    }

    @PostConstruct
    private void initializePoller() {
        ArgumentUtil.ensureNotNullNorEmpty("jobType", pollerProperties.getJobType());
        ArgumentUtil.ensureNotNullNorEmpty("correlationKey", pollerProperties.getCorrelationKey());
        ArgumentUtil.ensureNotNullNorEmpty("consumerProperties.topic.suffix",
            kafkaBinderProperties.getConsumerProperties().get("topic.suffix"));
        ArgumentUtil.ensureNotNullNorEmpty("producerProperties.topic.suffix",
            kafkaBinderProperties.getConsumerProperties().get("topic.suffix"));       

        int numberOfThread = this.zeebeClientProperties.getWorker().getThreads();

        for (int thread = 0; thread < numberOfThread; thread++) {
            createJobWorker(this.pollerProperties.getJobType(), new KafkaConnectJobHandler());
        }
    }
    
    private void createJobWorker(final String jobType, final JobHandler jobHandler) {

        final String name = this.zeebeClientProperties.getWorker().getDefaultName();
        final int maxJobsActive = this.zeebeClientProperties.getWorker().getMaxJobsActive();
        final Duration timeout = this.zeebeClientProperties.getJob().getTimeout();
        final Duration pollInterval = this.zeebeClientProperties.getJob().getPollInterval();
        final Duration requestTimeout = this.zeebeClientProperties.getRequestTimeout();
        
        final JobWorkerBuilderStep3 builder = this.zeebeClient
                .newWorker()
                .jobType(jobType)
                .handler(jobHandler)
                .name(name)
                .maxJobsActive(maxJobsActive)
                .timeout(timeout)
                .pollInterval(pollInterval)
                .requestTimeout(requestTimeout);
        final JobWorker jobWorker = builder.open();

        LOGGER.info("Register job worker: {}", jobWorker);
    }
    
    class KafkaConnectJobHandler implements JobHandler {

        @Override
        public void handle(final JobClient client, final ActivatedJob job) throws Exception {
            final Map<String, Object> variablesAsMap = job.getVariablesAsMap();

            final String correlationKey = (String) variablesAsMap.get(pollerProperties.getCorrelationKey());
            final String topicPrefix = job.getElementId();
            final String producerTopicSuffix = kafkaBinderProperties.getProducerProperties().get("topic.suffix");
            final String consumerTopicSuffix = kafkaBinderProperties.getConsumerProperties().get("topic.suffix");
            final String topicName = new StringBuilder().append(topicPrefix).append(producerTopicSuffix).toString();

            jobRepository.addJob(JobInfo.from(correlationKey, job.getProcessInstanceKey(), job.getKey(), job));

            if(!consumerRepository.addConsumerIfAbsent(topicPrefix)) {
                try {
                    final MessageHandler messageHandler = new ConsumerMessageHandler(jobRepository, objectMapper,
                        zeebeClient);
                    ConsumerBuilder.prepare(targetFactory, bindingService, messageHandler, topicPrefix).setTopicSuffix(
                        consumerTopicSuffix).build();
                } catch (Exception e) {
                    LOGGER.error("Error while building the consumer {}. Detail: ", topicPrefix, e);
                    consumerRepository.removeConsumer(topicPrefix);
                    LOGGER.debug("Error while building the consumer. So remove the consumer {}", topicPrefix);
                }
            }
            streamBridge.send(topicName, variablesAsMap);
        }
    }
}