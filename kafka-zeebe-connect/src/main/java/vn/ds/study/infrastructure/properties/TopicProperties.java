/*
 * Class: TopicProperties
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
package vn.ds.study.infrastructure.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.stream.kafka.binder")
public class TopicProperties {

    private Map<String, String> consumerProperties = new HashMap<>();

    private Map<String, String> producerProperties = new HashMap<>();

    public Map<String, String> getConsumerProperties() {
        return consumerProperties;
    }

    public void setConsumerProperties(Map<String, String> consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public Map<String, String> getProducerProperties() {
        return producerProperties;
    }

    public void setProducerProperties(Map<String, String> producerProperties) {
        this.producerProperties = producerProperties;
    }
}