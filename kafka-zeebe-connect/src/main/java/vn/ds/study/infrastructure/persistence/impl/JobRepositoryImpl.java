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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaExtendedBindingProperties;
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
import vn.ds.study.infrastructure.properties.PollerProperties;
import vn.ds.study.model.JobInfo;

@Repository
public class JobRepositoryImpl implements JobRepository{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRepositoryImpl.class);

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
    
    @PostConstruct
    @SuppressWarnings("unchecked")
    private void initialize() throws IOException {
        final Properties properties = new Properties();
        properties.putAll(kafkaBinderConfigurationProperties.mergedConsumerConfiguration());
        final KafkaConsumer<byte[], byte[]> kafkaConsumer = KafkaConsumerBuilder.prepare(properties).build();
        
        final long maxPollInterval = Long.parseLong((String) properties.get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG)); 
        final String topicName = "__job-instances";
        final Set<TopicPartition> assignment = kafkaConsumer.assignment();
        final Map<TopicPartition, Long> endOffsets = kafkaConsumer.endOffsets(assignment);

        while (!hasReadToEndOffsets(endOffsets, kafkaConsumer)) {
            try {
                ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(Duration.ofMillis(maxPollInterval));
                for (ConsumerRecord<byte[], byte[]> record : records) {

                    if (record.key() == null || record.value() == null) {
                        continue;
                    }
                    final String key = Serdes.String().deserializer().deserialize(topicName, record.headers(), record.key());
                    final ObjectReader reader = objectMapper.reader();
                    JsonNode jsonNode = reader.readTree(new ByteArrayInputStream((byte[]) record.value()));
                    final JobInfo jobInfo = objectMapper.treeToValue(jsonNode, JobInfo.class);

                    this.jobIntances.put(key, jobInfo);
                    LOGGER.info("Loaded message key {} and message value {}", key, jsonNode);
                }
            } catch (IOException e) {
                LOGGER.info("Error while loading messages from Kafka. Detail: ", e);
            }
        }
        kafkaConsumer.close();
    }

    private boolean hasReadToEndOffsets(
        final Map<TopicPartition, Long> endOffsets,
        final KafkaConsumer<byte[], byte[]> kafkaConsumer) {
        endOffsets.entrySet().removeIf(entry -> kafkaConsumer.position(entry.getKey()) >= entry.getValue());
        return endOffsets.isEmpty();
    }

    @Override
    public void addJob(JobInfo jobInfo) {
        this.jobIntances.put(jobInfo.getCorrelationKey(), jobInfo);
        Map<String, Object> jobInfoAsMap = objectMapper.convertValue(jobInfo, Map.class);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KafkaHeaders.MESSAGE_KEY, jobInfo.getCorrelationKey().getBytes());
        Message<?> message = MessageBuilder.createMessage(jobInfoAsMap, new MessageHeaders(headers));
        this.streamBridge.send("jobStorage-out-0", message);
    }

    @Override
    public JobInfo findJob(String correlationKey) {
        return this.jobIntances.remove(correlationKey);
    }
}