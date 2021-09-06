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
public class TopicConfiguration {

    @Bean("consumerTopicProperties")
    @ConfigurationProperties("spring.cloud.stream.kafka.binder.consumerProperties.topic")
    KafkaTopicProperties getConsumerTopicProperties() {
        return new KafkaTopicProperties();
    }
    
    @Bean("producerTopicProperties")
    @ConfigurationProperties("spring.cloud.stream.kafka.binder.producerProperties.topic")
    KafkaTopicProperties getProducerTopicProperties() {
        return new KafkaTopicProperties();
    }
    
}
