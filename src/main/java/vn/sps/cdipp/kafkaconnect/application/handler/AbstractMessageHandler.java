/*
 * Class: AbstractMessageHandler
 *
 * Created on Nov 1, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.sps.cdipp.kafkaconnect.application.handler;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

abstract class AbstractMessageHandler {
    
    private String responseWrapperKey;
    
    public AbstractMessageHandler(String responseWrapperKey) {
        super();
        this.responseWrapperKey = responseWrapperKey;
    }

    public AbstractMessageHandler() {
        super();
    }

    ObjectNode wrapResponseIfNecessary(final JsonNode jsonNode) {
        final ObjectNode objectNode;
        if(StringUtils.hasText(responseWrapperKey)) {
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode root = mapper.createObjectNode();
            objectNode = root.set(responseWrapperKey, jsonNode);
        }
        else {
            objectNode = (ObjectNode) jsonNode;
        }
        return objectNode;
    }
    
}
