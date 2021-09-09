/*
 * Class: JobPersistenceImpl
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
package vn.ds.study.infrastructure.persistence.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBindingProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaExtendedBindingProperties;
import org.springframework.cloud.stream.config.BinderProperties;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import vn.ds.study.application.builder.KafkaConsumerBuilder;
import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.infrastructure.properties.KafkaTopicProperties;
import vn.ds.study.infrastructure.properties.PollerProperties;
import vn.ds.study.model.JobInfo;

@Repository
public class JobRepositoryImpl implements JobRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRepositoryImpl.class);

    private static final String BINDING_NAME_DEFAULT = "job-stogare-in-0";

    private static final String TOPIC_NAME_DEFAULT = "__job-instances";
    
    private static final String GROUP_NAME_DEFAULT = "job-instance-manager";

    private Map<String, JobInfo> jobIntances = new ConcurrentHashMap<>();
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PollerProperties pollerProperties;

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties;

    @Autowired
    private KafkaExtendedBindingProperties kafkaExtendedBindingProperties;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;
    
    @Autowired
    private KafkaTopicProperties jobStorageTopicProperties;

    @PostConstruct
    @SuppressWarnings("unchecked")
    private void initialize() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final Properties properties = this.createProperties();
        final String topicName = jobStorageTopicProperties.getName() != null ? jobStorageTopicProperties.getName()
                : TOPIC_NAME_DEFAULT;
        final long maxPollInterval = Long.parseLong(
            (String) properties.getProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000"));

        this.createNewTopicIfNecessary(properties, topicName);
        
        final KafkaConsumer<byte[], byte[]> kafkaConsumer = KafkaConsumerBuilder.prepare(properties).build();

        final Map<Integer, TopicPartition> partitions = this.getTopicPartitions(topicName, kafkaConsumer);

        kafkaConsumer.assign(partitions.values());
        kafkaConsumer.seekToBeginning(partitions.values());

        final Set<TopicPartition> assignment = kafkaConsumer.assignment();
        final Map<TopicPartition, Long> endOffsets = kafkaConsumer.endOffsets(assignment);

        while (!hasReadToEndOffsets(endOffsets, kafkaConsumer)) {
            try {
                ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(Duration.ofMillis(maxPollInterval));
                for (ConsumerRecord<byte[], byte[]> record : records) {

                    if (record.key() == null || record.value() == null) {
                        continue;
                    }
                    final String key = Serdes.String().deserializer().deserialize(topicName, record.headers(),
                        record.key());
                    final ObjectReader reader = objectMapper.reader();
                    JsonNode jsonNode = reader.readTree(new ByteArrayInputStream((byte[]) record.value()));
                    JobInfo jobInfo = objectMapper.treeToValue(jsonNode, JobInfo.class);
                    
                    this.jobIntances.put(key, jobInfo);
                    LOGGER.info("Loaded message key {} and message value {}", key, jsonNode);
                }
            } catch (IOException e) {
                LOGGER.info("Error while loading messages from Kafka. Detail: ", e);
            }
        }
        kafkaConsumer.close();
    }

    private Map<Integer, TopicPartition> getTopicPartitions(
        final String topicName,
        final KafkaConsumer<byte[], byte[]> kafkaConsumer) {
        final Map<Integer, TopicPartition> partitions = new HashMap<>();
        for (final PartitionInfo partition : kafkaConsumer.partitionsFor(topicName)) {
            final TopicPartition topicPartition = new TopicPartition(topicName, partition.partition());
            partitions.put(partition.partition(), topicPartition);
        }
        return partitions;
    }

    private void createNewTopicIfNecessary(final Properties properties, final String topicName)
            throws InterruptedException, ExecutionException {
        try (Admin admin = Admin.create(properties)) {
            final ListTopicsResult listTopicsResult = admin.listTopics();
            final KafkaFuture<Set<String>> namesFutures = listTopicsResult.names();

            final Set<String> topicNames = namesFutures.get();

            if (!topicNames.contains(topicName)) {
                final int minPartition = this.kafkaBinderConfigurationProperties.getMinPartitionCount();
                final short replicationFactor = this.kafkaBinderConfigurationProperties.getReplicationFactor();
                final NewTopic topic = new NewTopic(topicName, minPartition, replicationFactor);
                topic.configs(this.kafkaExtendedBindingProperties.getBindings().get(
                    BINDING_NAME_DEFAULT).getConsumer().getTopic().getProperties());

                final CreateTopicsResult result = admin.createTopics(Collections.singleton(topic));

                final KafkaFuture<Void> future = result.values().get(topicName);
                future.get();
            }
        }
    }

    private Properties createProperties() {
        final BindingProperties bindingProperties = this.bindingServiceProperties.getBindingProperties(
            BINDING_NAME_DEFAULT);
        final KafkaBindingProperties kafkaBindingProperties = kafkaExtendedBindingProperties.getBindings().get(
            BINDING_NAME_DEFAULT);

        final Properties properties = new Properties();

        properties.putAll(kafkaBinderConfigurationProperties.mergedConsumerConfiguration());

        final Map<String, BinderProperties> binderProperties = this.bindingServiceProperties.getBinders();
        final Map<String, Object> binderAddresses = new HashMap<>();
        binderProperties.forEach((key, value) -> {
            this.flatten(null, value.getEnvironment(), binderAddresses);
        });
        binderAddresses.forEach((key, value) -> {
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, value);
        });
        
        if(bindingProperties != null && bindingProperties.getGroup() != null) {
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, bindingProperties.getGroup());
        }
        return properties;
    }

    private boolean hasReadToEndOffsets(
        final Map<TopicPartition, Long> endOffsets,
        final KafkaConsumer<byte[], byte[]> consumer) {
        endOffsets.entrySet().removeIf(entry -> consumer.position(entry.getKey()) >= entry.getValue());
        return endOffsets.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void flatten(String propertyName, Object value, Map<String, Object> binderProperties) {
        if (value instanceof Map) {
            ((Map<Object, Object>) value).forEach(
                (k, v) -> flatten((propertyName != null ? propertyName + "." : "") + k, v, binderProperties));
        } else {
            binderProperties.put(propertyName, value.toString());
        }
    }

    @Override
    public void addJob(JobInfo jobInfo) {
        this.jobIntances.put(jobInfo.getCorrelationKey(), jobInfo);
        Map<String, Object> jobInfoAsMap = objectMapper.convertValue(jobInfo, Map.class);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KafkaHeaders.MESSAGE_KEY, jobInfo.getCorrelationKey().getBytes());
        Message<?> message = MessageBuilder.createMessage(jobInfoAsMap, new MessageHeaders(headers));
        this.streamBridge.send(TOPIC_NAME_DEFAULT, message);
    }

    @Override
    public JobInfo findJob(String correlationKey) {
        return this.jobIntances.remove(correlationKey);
    }
}