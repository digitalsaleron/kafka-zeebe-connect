/*
 * Class: KafkaConnectJobHandler
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
package vn.ds.study.application.handler;

import java.util.Map;

import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import vn.ds.study.application.builder.ConsumerBuilder;
import vn.ds.study.infrastructure.persistence.ConsumerRepository;
import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.infrastructure.properties.PollerProperties;
import vn.ds.study.model.JobInfo;

public class KafkaConnectJobHandler implements JobHandler {
    
    private KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties;

    private PollerProperties pollerProperties;
    
    private StreamBridge streamBridge;
    
    private JobRepository jobRepository;
    
    private ConsumerRepository consumerRepository;
    
    public KafkaConnectJobHandler(KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties,
            PollerProperties pollerProperties, StreamBridge streamBridge, JobRepository jobRepository,
            ConsumerRepository consumerRepository) {
        super();
        this.kafkaBinderConfigurationProperties = kafkaBinderConfigurationProperties;
        this.pollerProperties = pollerProperties;
        this.streamBridge = streamBridge;
        this.jobRepository = jobRepository;
        this.consumerRepository = consumerRepository;
    }

    @Override
    public void handle(final JobClient client,final ActivatedJob job) throws Exception {
        final Map<String, Object> variablesAsMap = job.getVariablesAsMap();

        final String correlationKey = (String) variablesAsMap.get(pollerProperties.getCorrelationKey());
        final String prefixTopic = job.getElementId();
        final String suffixTopic = this.kafkaBinderConfigurationProperties.getProducerProperties().get("topic.suffix");
        final String topicName = new StringBuilder().append(prefixTopic).append(suffixTopic).toString();

        jobRepository.addJob(JobInfo.from(correlationKey, job.getProcessInstanceKey(), job.getKey(), job));
        
        if(!consumerRepository.containConsumer(prefixTopic)) {
            this.consumerRepository.addConsumer(prefixTopic);
        }
        
        streamBridge.send(topicName, variablesAsMap);
    }
}