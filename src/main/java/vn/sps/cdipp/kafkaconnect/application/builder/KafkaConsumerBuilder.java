/*
 * Class: DynamicConsumerManager
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
package vn.sps.cdipp.kafkaconnect.application.builder;

import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.StringUtils;

public final class KafkaConsumerBuilder {

    private static final String TOPIC_SUFFIX_DEFAULT = "-response";
    
    private static final String CONSUMER_NAME_SUFFIX_DEFAULT = "-in-0";

    private AbstractBindingTargetFactory<? extends MessageChannel> abstractBindingTargetFactory;

    private BindingService bindingService;

    private MessageHandler messageHandler;
    
    private String topicPrefix;

    private KafkaConsumerBuilder() {
    }

    private KafkaConsumerBuilder(AbstractBindingTargetFactory<? extends MessageChannel> abstractBindingTargetFactory,
            BindingService bindingService, MessageHandler messageHandler, String topicPrefix) {
        super();
        this.abstractBindingTargetFactory = abstractBindingTargetFactory;
        this.bindingService = bindingService;
        this.messageHandler = messageHandler;
        this.topicPrefix = topicPrefix;
    }

    public static ConsumerBuilder2 prepare(
        AbstractBindingTargetFactory<? extends MessageChannel> targetFactory,
        BindingService bindingService,
        MessageHandler messageHandler,
        String topicPrefix) {

        return (new KafkaConsumerBuilder()).new ConsumerBuilder2(targetFactory, bindingService, messageHandler, topicPrefix);
    }
    
    public static ConsumerBuilder3 prepare(Properties properties) {
        return (new KafkaConsumerBuilder()).new ConsumerBuilder3(properties);
    }
    
    public class ConsumerBuilder3 {

        private Properties properties;
        
        ConsumerBuilder3(Properties properties) {
            this.properties = properties;
        }
        
        @SuppressWarnings("rawtypes")
        public KafkaConsumer build() {
            return new KafkaConsumer<>(properties);
        }
    }

    public class ConsumerBuilder2 {

        private KafkaConsumerBuilder consumerBuilder;

        private String topicSuffix;
        
        private String group;
        
        private String consumerNameSuffix;
        
        ConsumerBuilder2(AbstractBindingTargetFactory<? extends MessageChannel> abstractBindingTargetFactory,
                BindingService bindingService, MessageHandler messageHandler, String topicPrefix) {
            this.consumerBuilder = new KafkaConsumerBuilder(abstractBindingTargetFactory, bindingService, messageHandler,
                topicPrefix);
        }

        public ConsumerBuilder2 setTopicSuffix(String topicSuffix) {
            this.topicSuffix = topicSuffix;
            return this;
        }

        public ConsumerBuilder2 setGroup(String group) {
            this.group = group;
            return this;
        }

        public ConsumerBuilder2 setConsumerNameSuffix(String consumerNameSuffix) {
            this.consumerNameSuffix = consumerNameSuffix;
            return this;
        }

        public SubscribableChannel build() {
            final String suffix = this.topicSuffix != null ? this.topicSuffix : TOPIC_SUFFIX_DEFAULT;
            final String consumerGroup = StringUtils.hasText(this.group) ? this.group : consumerBuilder.getTopicPrefix();
            final String consumerSuffix = StringUtils.hasText(this.consumerNameSuffix) ? this.consumerNameSuffix
                    : CONSUMER_NAME_SUFFIX_DEFAULT;

            final BindingService bindingService = consumerBuilder.getBindingService();
            final AbstractBindingTargetFactory<? extends MessageChannel> targetFactory = consumerBuilder.getAbstractBindingTargetFactory();
            final MessageHandler consumerHandler = consumerBuilder.getMessageHandler();
            final String topicPrefix = consumerBuilder.getTopicPrefix();
           
            final String topic = new StringBuilder().append(topicPrefix).append(suffix).toString();
            final String consumerName = new StringBuilder().append(topicPrefix).append(consumerSuffix).toString();

            
            final BindingProperties bindingProperties = new BindingProperties();
            bindingProperties.setConsumer(new ConsumerProperties());
            bindingProperties.setDestination(topic);
            bindingProperties.setGroup(consumerGroup);

            final BindingServiceProperties bindingServiceProperties = bindingService.getBindingServiceProperties();
            bindingServiceProperties.getBindings().put(consumerName, bindingProperties);

            SubscribableChannel channel = (SubscribableChannel) targetFactory.createInput(consumerName);
            bindingService.bindConsumer(channel, consumerName);

            channel.subscribe(consumerHandler);
            
            return channel;
        }
        
        
    }

    public AbstractBindingTargetFactory<? extends MessageChannel> getAbstractBindingTargetFactory() {
        return abstractBindingTargetFactory;
    }

    public BindingService getBindingService() {
        return bindingService;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }
}