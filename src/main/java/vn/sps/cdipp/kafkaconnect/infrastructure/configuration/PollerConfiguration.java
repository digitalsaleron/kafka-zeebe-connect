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
package vn.sps.cdipp.kafkaconnect.infrastructure.configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaExtendedBindingProperties;
import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1.JobWorkerBuilderStep3;
import io.camunda.zeebe.client.impl.ZeebeClientBuilderImpl;
import io.camunda.zeebe.client.impl.command.ArgumentUtil;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;
import vn.sps.cdipp.kafkaconnect.application.builder.KafkaConsumerBuilder;
import vn.sps.cdipp.kafkaconnect.application.builder.KafkaConsumerManager;
import vn.sps.cdipp.kafkaconnect.application.handler.ResponseMessageHandler;
import vn.sps.cdipp.kafkaconnect.application.utils.JsonMapFlattener;
import vn.sps.cdipp.kafkaconnect.infrastructure.persistence.JobRepository;
import vn.sps.cdipp.kafkaconnect.infrastructure.properties.KafkaTopicProperties;
import vn.sps.cdipp.kafkaconnect.infrastructure.properties.PollerProperties;
import vn.sps.cdipp.kafkaconnect.infrastructure.properties.Wrapper;
import vn.sps.cdipp.kafkaconnect.model.JobInfo;

@DependsOn(value = {"jobRepository"})
@Configuration
@EnableConfigurationProperties(
        value = { PollerProperties.class, KafkaBinderConfigurationProperties.class,
            KafkaExtendedBindingProperties.class, Wrapper.class })
public class PollerConfiguration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PollerConfiguration.class);

    private PollerProperties pollerProperties;

    private JobRepository jobRepository;

    private KafkaConsumerManager kafkaConsumerManager;
    
    private ZeebeClient zeebeClient;
    
    private ZeebeClientBuilderImpl zeebeClientBuilder;
    
    private StreamBridge streamBridge;
    
    private ObjectMapper objectMapper;
    
    private AbstractBindingTargetFactory<? extends MessageChannel> targetFactory;
    
    private BindingService bindingService;
    
    private KafkaTopicProperties consumerTopicProperties;
    
    private KafkaTopicProperties producerTopicProperties;
    
    private Wrapper wrapper;
    
    public PollerConfiguration(PollerProperties pollerProperties, JobRepository jobRepository,
            KafkaConsumerManager kafkaConsumerManager, ZeebeClientBuilderImpl zeebeClientBuilder,
            ZeebeClient zeebeClient, StreamBridge streamBridge, ObjectMapper objectMapper,
            AbstractBindingTargetFactory<? extends MessageChannel> targetFactory, BindingService bindingService,
            KafkaTopicProperties consumerTopicProperties, KafkaTopicProperties producerTopicProperties, Wrapper wrapper) {
        super();
        this.pollerProperties = pollerProperties;
        this.jobRepository = jobRepository;
        this.kafkaConsumerManager = kafkaConsumerManager;
        this.zeebeClientBuilder = zeebeClientBuilder;
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
        this.targetFactory = targetFactory;
        this.bindingService = bindingService;
        this.consumerTopicProperties = consumerTopicProperties;
        this.producerTopicProperties = producerTopicProperties;
        this.zeebeClient = zeebeClient;
        this.wrapper = wrapper;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        this.initializePoller();
    }

    private void initializePoller() {
        ArgumentUtil.ensureNotNullNorEmpty("jobType", pollerProperties.getJobType());
        ArgumentUtil.ensureNotNullNorEmpty("correlationKey", pollerProperties.getCorrelationKey());
        ArgumentUtil.ensureNotNullNorEmpty("consumerProperties.topic.suffix", consumerTopicProperties.getSuffix());
        ArgumentUtil.ensureNotNullNorEmpty("producerProperties.topic.suffix", producerTopicProperties.getSuffix());

        int numberOfThread = this.zeebeClientBuilder.getNumJobWorkerExecutionThreads();
                
        for (int thread = 0; thread < numberOfThread; thread++) {
            createJobWorker(this.pollerProperties.getJobType(), new KafkaConnectJobHandler());
        }
    }
    
    private void createJobWorker(final String jobType, final JobHandler jobHandler) {

        final String name = this.zeebeClientBuilder.getDefaultJobWorkerName();
        final int maxJobsActive = this.zeebeClientBuilder.getDefaultJobWorkerMaxJobsActive();
        final Duration timeout = this.zeebeClientBuilder.getDefaultJobTimeout();
        final Duration pollInterval = this.zeebeClientBuilder.getDefaultJobPollInterval();
        final Duration requestTimeout = this.zeebeClientBuilder.getDefaultRequestTimeout();
        
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
    
    class KafkaConnectJobHandler implements JobHandler {

        @Override
        public void handle(final JobClient client, final ActivatedJob job) throws Exception {
            final Map<String, Object> variablesAsMap = job.getVariablesAsMap();
            final String jobKeyAsString = String.valueOf(job.getKey());
            variablesAsMap.putIfAbsent("eventId", jobKeyAsString);

            final Map<String, Object> flattenedVariablesAsMap = JsonMapFlattener.flatten(variablesAsMap);

            job.getCustomHeaders().forEach((key, value) -> {
                flattenedVariablesAsMap.put(key, detectDataType(value));
                LOGGER.debug("Add value {} to path {} ", value, key);
            });
            
            final String correlationKey = (String) variablesAsMap.get(pollerProperties.getCorrelationKey());
            final String topicPrefix = detectTopicPrefix(job.getElementId());
            
            final String producerTopicSuffix = producerTopicProperties.getSuffix();
            final String consumerTopicSuffix = consumerTopicProperties.getSuffix();
            final String topicName = new StringBuilder().append(topicPrefix).append(producerTopicSuffix).toString();
            final String consumerName = topicPrefix;

            JsonNode node = objectMapper.convertValue(job, JsonNode.class);
            vn.sps.cdipp.kafkaconnect.model.ActivatedJob activatedJob = objectMapper.treeToValue(node,
                vn.sps.cdipp.kafkaconnect.model.ActivatedJob.class);

            jobRepository.addJob(JobInfo.from(correlationKey, job.getProcessInstanceKey(), job.getKey(), activatedJob));

            final MessageHandler messageHandler = new ResponseMessageHandler(jobRepository, objectMapper, zeebeClient,
                pollerProperties.getCorrelationKey(), wrapper.getResponseWrapperKey());
            if (!kafkaConsumerManager.findAndAddConsumerIfAbsent(consumerName, messageHandler)) {
                try {
                    KafkaConsumerBuilder.prepare(targetFactory, bindingService, messageHandler,
                        topicPrefix).setTopicSuffix(consumerTopicSuffix).build();
                    LOGGER.info("Created consumer {} to consume topic {}", consumerName, topicName);
                } catch (Exception e) {
                    LOGGER.error("Error while building the consumer {}. Detail: ", topicPrefix, e);
                    kafkaConsumerManager.removeConsumer(consumerName);
                    LOGGER.debug("Error while building the consumer. So remove the consumer {}", topicPrefix);
                }
            }
            final JsonNode jsonNode = convertProperties2JsonNode(flattenedVariablesAsMap);
            
            LOGGER.debug("Send the message to Kafka {}", jsonNode.toPrettyString());
            streamBridge.send(topicName, jsonNode);
            LOGGER.info("Send the message {} - step {} to Kafka", correlationKey, job.getElementId());
        }

        private JsonNode convertProperties2JsonNode(final Map<String, Object> variablesAsMap) throws IOException {
            final String jsonAsString = new PropertiesToJsonConverter().convertFromValuesAsObjectMap(variablesAsMap);
            return objectMapper.readTree(jsonAsString);
        }
        
        private String detectTopicPrefix(String jobElementId) {
            if (consumerTopicProperties.isPrefixIsPattern()) {
                final String regex = consumerTopicProperties.getPrefix();
                final String string = jobElementId;

                final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                final Matcher matcher = pattern.matcher(string);
                matcher.find();
                return matcher.group(1);
            } else {
                return jobElementId;
            }
        }

        private Object detectDataType(String value) {
            if (!StringUtils.hasText(value)) {
                LOGGER.debug("The value is {}", value);
                return "";
            }
            if (value.matches("true") || value.matches("false")) {
                LOGGER.debug("Data type is {} and value is {}", Boolean.class, value);
                return Boolean.parseBoolean(value);
            }
            if(NumberUtils.isParsable(value)) {
                LOGGER.debug("Data type is {} and value is {}", Number.class, value);
                return NumberUtils.createNumber(value);
            }
            LOGGER.debug("Data type is {} and value is {}", String.class, value);
            return value.replace("\"", "");
        }
    }
}