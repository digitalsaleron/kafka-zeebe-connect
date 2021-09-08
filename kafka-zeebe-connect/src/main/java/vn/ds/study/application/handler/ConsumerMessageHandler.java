/*
 * Class: ConsumerMessageHandler
 *
 * Created on Sep 2, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.application.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.model.JobInfo;

public class ConsumerMessageHandler implements MessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerMessageHandler.class);

    private JobRepository jobRepository;

    private ObjectMapper objectMapper;

    private ZeebeClient client;
    
    private String correlationKey;
    
    public ConsumerMessageHandler(JobRepository jobRepository, ObjectMapper objectMapper, ZeebeClient client,
            String correlationKey) {
        super();
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
        this.client = client;
        this.correlationKey = correlationKey;
    }

    @Override
    public void handleMessage(Message<?> message) {
        final ObjectReader reader = objectMapper.reader();
        try {
            JsonNode jsonNode = reader.readTree(new ByteArrayInputStream((byte[]) message.getPayload()));
            final ObjectNode objectNode = (ObjectNode) jsonNode;
            final String key = objectNode.get(correlationKey).asText();

            final JobInfo jobI = jobRepository.findJob(key);
            final ActivatedJob job = jobI.getActivatedJob();

            final Map<String, Object> variables = objectMapper.convertValue(objectNode,
                new TypeReference<Map<String, Object>>() {
                });
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        } catch (IOException e) {
            LOGGER.error("Error while consuming the message. Detail: ", e);
        }
    }
}
