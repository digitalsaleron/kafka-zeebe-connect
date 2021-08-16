package vn.ds.study.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import vn.ds.study.model.ResolvedTicketResult;
import vn.ds.study.model.TicketRequest;
import vn.ds.study.service.TicketService;

@Component
public class ZeebeTicketResolvingHandler {

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(ZeebeTicketResolvingHandler.class);

	@Autowired
	private TicketService ticketService;

	@ZeebeWorker(type = "approvedTickets")
	public void handleJobFoo(final JobClient client, final ActivatedJob job) {

		Map<String, Object> variables = job.getVariablesAsMap();

		String ticketId = (String) variables.get("ticketId");
		String type = (String) variables.get("type");
		int amount = (int) variables.get("amount");
		int totalCostAmount = (int) variables.get("totalCostAmount");

		LOGGER.info(
		        "Resolve ticket request type = {}, amount = {}, totalCostAmount = {}",
		        type, amount, totalCostAmount);
		TicketRequest ticket = TicketRequest.from(ticketId, type, amount,
		        totalCostAmount);
		ResolvedTicketResult res = ticketService.resolve(ticket);

		variables.put("isResolved", res.isResolved());
		variables.put("resolution", res.getResolution());

		client.newCompleteCommand(job.getKey()).variables(variables).send();

	}

}