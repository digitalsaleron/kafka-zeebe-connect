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
package vn.sps.cdipp.kafkaconnect.application.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
import vn.sps.cdipp.kafkaconnect.application.exception.JobInstanceNotFoundException;
import vn.sps.cdipp.kafkaconnect.infrastructure.persistence.JobRepository;
import vn.sps.cdipp.kafkaconnect.model.ActivatedJob;
import vn.sps.cdipp.kafkaconnect.model.JobInfo;

public class ResponseMessageHandler extends AbstractMessageHandler implements MessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseMessageHandler.class);

    private JobRepository jobRepository;

    private ObjectMapper objectMapper;

    private ZeebeClient client;

    private String correlationKey;

    public ResponseMessageHandler(JobRepository jobRepository, ObjectMapper objectMapper, ZeebeClient client,
            String correlationKey, String responseWrapperKey) {
        super(responseWrapperKey);
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
            key = jsonNode.get(correlationKey).asText();
            final ObjectNode objectNode = this.wrapResponseIfNecessary(jsonNode);

            final JobInfo jobInfo = jobRepository.getJob(key);
            this.validateJobInfo(jobInfo, key);
            final ActivatedJob job = jobInfo.getActivatedJob();

            final Map<String, Object> variables = objectMapper.convertValue(objectNode,
                new TypeReference<Map<String, Object>>() {
                });
            client.newCompleteCommand(job.getKey()).variables(variables).send();
            LOGGER.debug("Send the message to Workflow {}", objectNode.toPrettyString());
            LOGGER.info("Receive and send the message {} - step {} to the workflow engine", key,
                jobInfo.getActivatedJob().getElementId());
        } 
        catch (IOException | JobInstanceNotFoundException e) {
            LOGGER.error("Error while responding the message with key {}. Detail: ", key, e);
        }
    }

    private void validateJobInfo(JobInfo jobInfo, String key) throws JobInstanceNotFoundException {
        if (jobInfo == null) {
            throw new JobInstanceNotFoundException(String.format("The job instance %s could not be found", key));
        }
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.correlationKey).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResponseMessageHandler)) {
            return false;
        }
        ResponseMessageHandler that = (ResponseMessageHandler) obj;
        return new EqualsBuilder().append(this.correlationKey, that.correlationKey).isEquals();
    }
}