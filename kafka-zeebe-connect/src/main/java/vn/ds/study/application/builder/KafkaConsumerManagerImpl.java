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

import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;

@Component("kafkaConsumerManager")
public class KafkaConsumerManagerImpl implements KafkaConsumerManager{

    private Map<String, SubscribableChannel> store = new ConcurrentHashMap<>();
    
    @Override
    public boolean findAndAddConsumerIfAbsent(String consumerName) {
        final SubscribableChannel previousValue = this.store.putIfAbsent(consumerName, new DirectWithAttributesChannel());
        return previousValue != null;
    }
    
    @Override
    public void addBindedConsumer(String consumerName, SubscribableChannel channel) {
        this.store.put(consumerName, channel);
    }
    

    @Override
    public SubscribableChannel removeConsumer(String consumerName) {
        return this.store.remove(consumerName);
    }

}