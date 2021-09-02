package vn.ds.study.controller;

import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import vn.ds.study.model.JobInfo;
import vn.ds.study.service.JobService;

public class Kafka2ZeebeIntegrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kafka2ZeebeIntegrator.class);

    @Autowired
    private ZeebeClient client;

    @Autowired
    private JobService jobService;
    
    Consumer<JsonNode> validatingRequests() {
        return data -> {
            ObjectNode objectNode = ((ObjectNode) data);
            String ticketId = objectNode.get("ticketId").asText();
            
            JobInfo jobI = jobService.find(ticketId);
            ActivatedJob job = jobI.getActivatedJob();

            Map<String, Object> variables = job.getVariablesAsMap();
            variables.put("isValid", true);

            LOGGER.info("Consume validated ticketId {} from kafka and send to zeebe", variables.get("ticketId"));

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        };
    }

    Consumer<JsonNode> waitingForApprovalTickets() {
        return data -> {
            ObjectNode objectNode = ((ObjectNode) data);
            String ticketId = objectNode.get("ticketId").asText();
            
            JobInfo jobI = jobService.find(ticketId);
            ActivatedJob job = jobI.getActivatedJob();

            Map<String, Object> variables = job.getVariablesAsMap();

            LOGGER.info("Consume waiting for approval validated ticketId {} from kafka and send to zeebe",
                variables.get("ticketId"));

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        };
    }

    Consumer<JsonNode> approvedTickets() {
        return data -> {
            ObjectNode objectNode = ((ObjectNode) data);
            String ticketId = objectNode.get("ticketId").asText();
            
            JobInfo jobI = jobService.find(ticketId);
            ActivatedJob job = jobI.getActivatedJob();

            Map<String, Object> variables = job.getVariablesAsMap();

            LOGGER.info("Consume approval ticketId {} from kafka and send to zeebe",
                variables.get("ticketId"));
            
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        };
    }
}