package vn.ds.study.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.cloud.stream.binding.AbstractBindingTargetFactory;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import vn.ds.study.model.JobInfo;
import vn.ds.study.service.JobService;

@Component
public class DynamicKafkaZeebeConnect {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicKafkaZeebeConnect.class);

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private JobService jobService;

    @Autowired
    private BindingService bindingService;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private AbstractBindingTargetFactory<? extends MessageChannel> bindingTargetFactory;

    @Autowired
    private ZeebeClient client;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> createdDynamicConsumer = new ConcurrentHashMap<>();

    public DynamicKafkaZeebeConnect() {
        System.out.println("...");
    }
    
    @PostConstruct
    void post() {
        System.out.println();
    }

    //    @ZeebeWorker(type = "ticketProcessing")
    public void ticketProcessing(final JobClient client, final ActivatedJob job) {

        Map<String, Object> variables = job.getVariablesAsMap();
        String ticketId = (String) variables.get("ticketId");
        String prefixTopic = job.getElementId();
        String topic = prefixTopic + "-requests";
        jobService.addJob(JobInfo.from(ticketId, job.getProcessInstanceKey(), job.getKey(), job));

        if (!createdDynamicConsumer.containsKey(prefixTopic)) {
            this.createDynamicConsumer(prefixTopic);
            this.createdDynamicConsumer.put(prefixTopic, prefixTopic);
        }
        streamBridge.send(topic, variables);
    }

    void createDynamicConsumer(String prefixTopic) {

        String topic = prefixTopic + "-responses";
        String consumerGroup = prefixTopic;
        String consumerName = prefixTopic;

        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setMaxAttempts(1);
        BindingProperties bindingProperties = new BindingProperties();
        bindingProperties.setConsumer(consumerProperties);
        bindingProperties.setDestination(topic);
        bindingProperties.setGroup(consumerGroup);

        BindingServiceProperties bindingServiceProperties = this.bindingService.getBindingServiceProperties();

        bindingServiceProperties.getBindings().put(consumerName, bindingProperties);
        SubscribableChannel channel = (SubscribableChannel) bindingTargetFactory.createInput(consumerName);
        beanFactory.registerSingleton(consumerName, channel);
        channel = (SubscribableChannel) beanFactory.initializeBean(channel, consumerName);
        bindingService.bindConsumer(channel, consumerName);
        channel.subscribe(new DynamicConsumerHandler());
    }

    class DynamicConsumerHandler implements MessageHandler {

        @Override
        public void handleMessage(Message<?> message) {
            final ObjectReader reader = objectMapper.reader();
            JsonNode newNode;
            try {
                newNode = reader.readTree(new ByteArrayInputStream((byte[]) message.getPayload()));
                ObjectNode objectNode = (ObjectNode) newNode;
                String ticketId = objectNode.get("ticketId").asText();

                JobInfo jobI = jobService.find(ticketId);
                ActivatedJob job = jobI.getActivatedJob();

                Map<String, Object> variables = objectMapper.convertValue(objectNode,
                    new TypeReference<Map<String, Object>>() {
                    });

                client.newCompleteCommand(job.getKey()).variables(variables).send();
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        }
    }
}