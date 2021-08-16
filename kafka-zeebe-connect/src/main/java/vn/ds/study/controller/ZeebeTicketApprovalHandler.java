package vn.ds.study.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import vn.ds.study.model.TicketRequest;

@Component
public class ZeebeTicketApprovalHandler {

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(ZeebeTicketApprovalHandler.class);

	Map<String, TicketRequest> waitingRequests = new HashMap<>();

	
	List<TicketRequest> getTickets(int firstMax) {
		if (firstMax <= 0) {
			return waitingRequests.values().stream()
			        .collect(Collectors.toList());
		}
		return waitingRequests.values().stream().limit(firstMax)
		        .collect(Collectors.toList());
	}

	@ZeebeWorker(type = "waitingForApprovalTickets")
	public void handleJobFoo(final JobClient client, final ActivatedJob job) {

		Map<String, Object> variables = job.getVariablesAsMap();

		String ticketId = (String) variables.get("ticketId");
		String type = (String) variables.get("type");
		int amount = (int) variables.get("amount");
		int totalCostAmount = (int) variables.get("totalCostAmount");

		LOGGER.info(
		        "Ticket request id = {}, type = {}, amount = {}, totalCostAmount = {} is waiting for approval",
		        ticketId, type, amount, totalCostAmount);
		TicketRequest ticket = TicketRequest.from(ticketId, type, amount,
		        totalCostAmount);
		waitingRequests.put(ticketId, ticket);

		client.newCompleteCommand(job.getKey()).variables(variables).send();

	}

}