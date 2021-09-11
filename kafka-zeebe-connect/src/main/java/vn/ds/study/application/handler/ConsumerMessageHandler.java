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
import vn.ds.study.application.exception.JobInstanceNotFoundException;
import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.model.ActivatedJob;
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
    public void handleMessage(final Message<?> message) {
        final ObjectReader reader = objectMapper.reader();
        String key = null;
        try {
            final JsonNode jsonNode = reader.readTree(new ByteArrayInputStream((byte[]) message.getPayload()));
            final ObjectNode objectNode = (ObjectNode) jsonNode;
            key = objectNode.get(correlationKey).asText();

            final JobInfo jobInfo = jobRepository.getJob(key);
            this.validateJobInfo(jobInfo, key);
            final ActivatedJob job = jobInfo.getActivatedJob();

            final Map<String, Object> variables = objectMapper.convertValue(objectNode,
                new TypeReference<Map<String, Object>>() {
                });
            client.newCompleteCommand(job.getKey()).variables(variables).send();
            LOGGER.info("Send the job instance {} to the workflow engine", key);
        } catch (IOException | JobInstanceNotFoundException e) {
            LOGGER.error("Error while responding the message with correlation key {}. Detail: ", key, e);
        }
    }

    private void validateJobInfo(JobInfo jobInfo, String key) throws JobInstanceNotFoundException {
        if (jobInfo == null) {
            throw new JobInstanceNotFoundException(String.format("The job instance %s could not be found", key));
        }
    }
}
