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
package vn.sps.cdipp.kafkaconnect.infrastructure.persistence.impl;

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

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import vn.sps.cdipp.kafkaconnect.application.builder.KafkaConsumerBuilder;
import vn.sps.cdipp.kafkaconnect.infrastructure.persistence.JobRepository;
import vn.sps.cdipp.kafkaconnect.infrastructure.persistence.JobRepositoryJmxMBean;
import vn.sps.cdipp.kafkaconnect.infrastructure.properties.JobStorageProperties;
import vn.sps.cdipp.kafkaconnect.infrastructure.properties.KafkaTopicProperties;
import vn.sps.cdipp.kafkaconnect.model.JobInfo;
import vn.sps.cdipp.kafkaconnect.model.event.ConsumerRecoveryEvent;

@Repository("jobRepository")
public class JobRepositoryImpl implements JobRepository, JobRepositoryJmxMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRepositoryImpl.class);

    private static final String DEFAULT_BINDING_NAME = "job-storage-in-0";

    private static final String DEFAULT_TOPIC_NAME = "__job-instances";

    private static final String DEFAULT_GROUP_NAME = "job-instance-manager";

    private Map<String, JobInfo> jobIntances = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private KafkaTopicProperties jobStorageTopicProperties;
    
    @Autowired
    private JobStorageProperties jobStorageProperties;
    
    @Autowired
    private KafkaExtendedBindingProperties kafkaExtendedBindingProperties;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    private String topicName;

    @PostConstruct
    @SuppressWarnings("unchecked")
    private void initialize() throws IOException, InterruptedException, ExecutionException {
        final Properties properties = this.createProperties();
        topicName = jobStorageTopicProperties.getName() != null ? jobStorageTopicProperties.getName()
                : DEFAULT_TOPIC_NAME;
        final long maxPollInterval = Long.parseLong(
            (String) properties.getProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000"));

        this.createNewTopicIfNecessary(properties, topicName);

        final KafkaConsumer<byte[], byte[]> kafkaConsumer = KafkaConsumerBuilder.prepare(properties).build();

        final Map<Integer, TopicPartition> partitions = this.getTopicPartitions(topicName, kafkaConsumer);

        kafkaConsumer.assign(partitions.values());
        kafkaConsumer.seekToBeginning(partitions.values());

        final Set<TopicPartition> assignment = kafkaConsumer.assignment();
        final Map<TopicPartition, Long> endOffsets = kafkaConsumer.endOffsets(assignment);

        try {
            while (!hasReadToEndOffsets(endOffsets, kafkaConsumer)) {
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
                    LOGGER.debug("Loaded message key {} and message value {}", key, jsonNode);
                }
            }
            LOGGER.info("Completed job storage initialization. Loaded {} job instances from Kafka",
                this.jobIntances.size());
            this.eventPublisher.publishEvent(new ConsumerRecoveryEvent(this, Collections.unmodifiableMap(jobIntances)));
        } catch (Exception e) {
            LOGGER.error("Error while loading messages from Kafka. Detail: ", e);
            if(!this.jobStorageProperties.isExceptionIgnoreEnabled()) {
                throw e;
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
                final Map<String, String> configs = new HashMap<>();
                configs.put(TopicConfig.CLEANUP_POLICY_CONFIG,
                    properties.getProperty(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT));
                configs.put(TopicConfig.DELETE_RETENTION_MS_CONFIG,
                    properties.getProperty(TopicConfig.DELETE_RETENTION_MS_CONFIG, "0"));
                configs.put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG,
                    properties.getProperty(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.5"));
                configs.put(TopicConfig.SEGMENT_MS_CONFIG,
                    properties.getProperty(TopicConfig.SEGMENT_MS_CONFIG, "3600000"));
                configs.put(TopicConfig.SEGMENT_BYTES_CONFIG,
                    properties.getProperty(TopicConfig.SEGMENT_BYTES_CONFIG, "1048576"));
                configs.put(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG,
                    properties.getProperty(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, "0"));
                final NewTopic topic = new NewTopic(topicName, minPartition, replicationFactor).configs(configs);

                LOGGER.debug("Create the topic properties {}", configs);
                final CreateTopicsResult result = admin.createTopics(Collections.singleton(topic));

                final KafkaFuture<Void> future = result.values().get(topicName);
                future.get();
            }
        }
    }

    private Properties createProperties() {
        final BindingProperties bindingProperties = this.bindingServiceProperties.getBindingProperties(
            DEFAULT_BINDING_NAME);
        
        final KafkaBindingProperties kafkaBindingProperties = this.kafkaExtendedBindingProperties.getBindings().get(
            DEFAULT_BINDING_NAME);

        final Properties properties = new Properties();

        properties.putAll(kafkaBinderConfigurationProperties.mergedConsumerConfiguration());

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        final Map<String, BinderProperties> binderProperties = this.bindingServiceProperties.getBinders();
        final Properties binderAddresses = new Properties();
        binderProperties.forEach((key, value) -> {
            this.flatten(null, value.getEnvironment(), binderAddresses);
        });
        
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            binderAddresses.getOrDefault("spring.cloud.stream.kafka.binder.brokers", "localhost:9092"));
        properties.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, binderAddresses.getOrDefault(
            "spring.cloud.stream.kafka.binder.configuration.security.protocol", "PLAINTEXT"));

        if (bindingProperties != null && bindingProperties.getGroup() != null) {
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, bindingProperties.getGroup());
        } else {
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, DEFAULT_GROUP_NAME);
        }
        if (kafkaBindingProperties != null && kafkaBindingProperties.getConsumer() != null
                && kafkaBindingProperties.getConsumer().getTopic() != null) {
            properties.putAll(kafkaBindingProperties.getConsumer().getTopic().getProperties());
        }
        LOGGER.debug("Create the Kafka consumer properties {}", properties);
        return properties;
    }

    private boolean hasReadToEndOffsets(
        final Map<TopicPartition, Long> endOffsets,
        final KafkaConsumer<byte[], byte[]> consumer) {
        endOffsets.entrySet().removeIf(entry -> consumer.position(entry.getKey()) >= entry.getValue());
        return endOffsets.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void flatten(String propertyName, Object value, Properties binderProperties) {
        if (value instanceof Map) {
            ((Map<Object, Object>) value).forEach(
                (k, v) -> flatten((propertyName != null ? propertyName + "." : "") + k, v, binderProperties));
        } else {    
            binderProperties.put(propertyName, value.toString());
        }
    }

    @Override
    public void addJob(JobInfo jobInfo) {

        this.jobIntances.compute(jobInfo.getCorrelationKey(), (key, value) -> {

            final Map<String, Object> headers = new HashMap<>();
            headers.put(KafkaHeaders.MESSAGE_KEY, jobInfo.getCorrelationKey().getBytes());
            final Message<?> message = MessageBuilder.createMessage(jobInfo, new MessageHeaders(headers));

            this.streamBridge.send(this.topicName, message);
            return jobInfo;
        });
        LOGGER.info("Add job instance {} to the job storage", jobInfo.getCorrelationKey());
    }

    @Override
    public JobInfo getJob(final String correlationKey) {

        final JobInfo jobInfo = this.jobIntances.remove(correlationKey);

        if (this.jobStorageProperties.isJobRemovalEnabled()) {
            this.streamBridge.send(this.topicName,
                new ProducerRecord<>(this.topicName, correlationKey.getBytes(), null));
            LOGGER.info("Get and remove job instance {} from the job storage", correlationKey);
        } else {
            LOGGER.info("Get job instance {} from the job storage", correlationKey);
        }
        return jobInfo;
    }

    @Override
    public long size() {
        return this.jobIntances.size();
    }

    @Override
    public void reload() {
        try {
            this.initialize();
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOGGER.error("Error while initializing the job storage. Detail: ", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean containsKey(String key) {
        return this.jobIntances.containsKey(key);
    }
}