package vn.ds.study.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import vn.ds.study.model.JobInfo;
import vn.ds.study.model.TicketRequest;
import vn.ds.study.service.JobService;

@Component
public class Zeebe2KafkaIntegrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Zeebe2KafkaIntegrator.class);

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private JobService jobService;

    @ZeebeWorker(type = "validatingTickets")
    public void validatingTickets(final JobClient client, final ActivatedJob job) {

        Map<String, Object> variables = job.getVariablesAsMap();

        variables.put("isValid", false);

        String ticketId = (String) variables.get("ticketId");
        String type = (String) variables.get("type");
        int amount = (int) variables.get("amount");
        int totalCostAmount = (int) variables.get("totalCostAmount");

        LOGGER.info("Bridge ticket validation request with id = {}", ticketId);
        jobService.addJob(JobInfo.from(ticketId, job.getProcessInstanceKey(), job.getKey(), job));

        streamBridge.send("validatingRequests-out-0", TicketRequest.from(ticketId, type, amount, totalCostAmount));
    }

    @ZeebeWorker(type = "waitingForApprovalTickets")
    public void waitingForApprovalTickets(final JobClient client, final ActivatedJob job) {

        Map<String, Object> variables = job.getVariablesAsMap();
        String ticketId = (String) variables.get("ticketId");
        
        jobService.addJob(JobInfo.from(ticketId, job.getProcessInstanceKey(), job.getKey(), job));
        this.streamBridge.send("waitingForApprovalTickets-out-0", variables);
    }
    

    @ZeebeWorker(type = "approvedTickets")
    public void approvedTickets(final JobClient client, final ActivatedJob job) {

        Map<String, Object> variables = job.getVariablesAsMap();
        String ticketId = (String) variables.get("ticketId");
        
        jobService.addJob(JobInfo.from(ticketId, job.getProcessInstanceKey(), job.getKey(), job));
        this.streamBridge.send("approvedTickets-out-0", variables);
    }

}