/*
 * Class: IntermediateMessageHandler
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
package vn.sps.cdipp.kafkaconnect.application.handler;

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

public class IntermediateMessageHandler implements MessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartMessageHandler.class);

    private ObjectMapper objectMapper;

    private ZeebeClient zeebeClient;

    private String correlationKey;
    
    private String messageName;

    public IntermediateMessageHandler(ObjectMapper objectMapper, ZeebeClient zeebeClient, String correlationKey,
            String messageName) {
        super();
        this.objectMapper = objectMapper;
        this.zeebeClient = zeebeClient;
        this.correlationKey = correlationKey;
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
            final String correlationKeyAsString = (String) variables.get(this.correlationKey);

            this.zeebeClient.newPublishMessageCommand().messageName(messageName).correlationKey(
                correlationKeyAsString).variables(variables).send();

            LOGGER.debug("Send the message to workflow {}", jsonNode.toPrettyString());
            LOGGER.info("Send a intermediate message {} - correlation key {} to workflow", messageName,
                correlationKeyAsString);
        } catch (Exception e) {
            LOGGER.error("Error while responding the intermediate message. Detail: ", e);
        }
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.messageName).append(this.correlationKey).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntermediateMessageHandler)) {
            return false;
        }
        IntermediateMessageHandler that = (IntermediateMessageHandler) obj;
        return new EqualsBuilder().append(this.messageName, that.messageName).append(this.correlationKey,
            that.correlationKey).isEquals();
    }
}