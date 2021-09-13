/*
 * Class: TopicConfiguration
 *
 * Created on Sep 7, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.ds.study.infrastructure.properties.KafkaTopicProperties;

@Configuration
public class KafkaTopicConfiguration {

    @Bean("consumerTopicProperties")
    @ConfigurationProperties(prefix = "spring.cloud.stream.kafka.binder.consumer-properties.topic")
    KafkaTopicProperties getConsumerTopicProperties() {
        return new KafkaTopicProperties();
    }
    
    @Bean("producerTopicProperties")
    @ConfigurationProperties(prefix = "spring.cloud.stream.kafka.binder.producer-properties.topic")
    KafkaTopicProperties getProducerTopicProperties() {
        return new KafkaTopicProperties();
    }
    
    @Bean("jobStorageTopicProperties")
    @ConfigurationProperties(prefix = "spring.cloud.stream.kafka.job-storage-in-0.consumer.topic.properties")
    KafkaTopicProperties getJobStorageTopicProperties() {
        return new KafkaTopicProperties();
    }
}