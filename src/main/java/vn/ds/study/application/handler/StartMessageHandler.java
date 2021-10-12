/*
 * Class: TriggerMessageHandler
 *
 * Created on Sep 30, 2021
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

import io.camunda.zeebe.client.ZeebeClient;

public class StartMessageHandler implements MessageHandler{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StartMessageHandler.class);
    
    private ZeebeClient zeebeClient;
    
    private ObjectMapper objectMapper;
    
    private String messageName;
    
    public StartMessageHandler(ZeebeClient zeebeClient, ObjectMapper objectMapper, String messageName) {
        super();
        this.zeebeClient = zeebeClient;
        this.objectMapper = objectMapper;
        this.messageName = messageName;
    }

    @Override
    public void handleMessage(Message<?> message) {
        final ObjectReader reader = objectMapper.reader();
        try {
            final JsonNode jsonNode = reader.readTree(new ByteArrayInputStream((byte[]) message.getPayload()));
            final Map<String, Object> variables = objectMapper.convertValue(jsonNode,
                new TypeReference<Map<String, Object>>() {
                });
            this.zeebeClient.newPublishMessageCommand().messageName(messageName).correlationKey("").variables(
                variables).send();
            LOGGER.debug("Send the message to workflow {}", jsonNode.toPrettyString());
            LOGGER.info("Send the start message processId {} to workflow", messageName);
        } catch (Exception e) {
            LOGGER.error("Error while responding the start message of processId {}. Detail: ", messageName, e);
        }
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.messageName).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StartMessageHandler)) {
            return false;
        }
        StartMessageHandler that = (StartMessageHandler) obj;
        return new EqualsBuilder().append(this.messageName, that.messageName).isEquals();
    }
}