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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1.JobWorkerBuilderStep3;
import io.camunda.zeebe.client.impl.command.ArgumentUtil;
import io.camunda.zeebe.spring.client.properties.ZeebeClientConfigurationProperties;
import vn.ds.study.application.handler.KafkaConnectJobHandler;
import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.infrastructure.properties.PollerProperties;

@Configuration
@EnableConfigurationProperties(value = { PollerProperties.class, KafkaBinderConfigurationProperties.class})
public class PollerConfiguration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PollerConfiguration.class);

    private ZeebeClientConfigurationProperties zeebeClientProperties;

    private PollerProperties pollerProperties;

    private JobRepository jobRepository;
    
    private ZeebeClient zeebeClient;
    
    private ZeebeClientBuilder zeebeClientBuilder;
    
    private StreamBridge streamBridge;
    
    private KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties;

    public PollerConfiguration(ZeebeClientConfigurationProperties zeebeClientProperties,
            PollerProperties pollerProperties, KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties,
            JobRepository jobRepository, ZeebeClientBuilder zeebeClientBuilder, StreamBridge streamBridge) {
        super();
        this.zeebeClientProperties = zeebeClientProperties;
        this.pollerProperties = pollerProperties;
        this.jobRepository = jobRepository;
        this.zeebeClientBuilder = zeebeClientBuilder;
        this.streamBridge = streamBridge;
        this.kafkaBinderConfigurationProperties = kafkaBinderConfigurationProperties;
        this.zeebeClient = this.zeebeClientBuilder.build();
    }

    @PostConstruct
    private void initializePoller() {
        ArgumentUtil.ensureNotNullNorEmpty("jobType", pollerProperties.getJobType());
        ArgumentUtil.ensureNotNullNorEmpty("correlationKey", pollerProperties.getCorrelationKey());
        ArgumentUtil.ensureNotNullNorEmpty("consumerProperties.topic.suffix",
            kafkaBinderConfigurationProperties.getConsumerProperties().get("topic.suffix"));

        int numberOfThread = this.zeebeClientProperties.getWorker().getThreads();
        
        final JobHandler jobHandler = new KafkaConnectJobHandler(kafkaBinderConfigurationProperties, pollerProperties,
            streamBridge, jobRepository);

        for (int thread = 0; thread < numberOfThread; thread++) {
            createJobWorker(this.pollerProperties.getJobType(), jobHandler);
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
}