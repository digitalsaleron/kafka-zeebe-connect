package vn.ds.study.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;

//@Component
public class ZeebeRejectedTicketHandler {

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(ZeebeRejectedTicketHandler.class);

//	@ZeebeWorker(type = "rejectedTickets")
	public void handleJobFoo(final JobClient client, final ActivatedJob job) {

		Map<String, Object> variables = job.getVariablesAsMap();

		String type = (String) variables.get("type");
		int amount = (int) variables.get("amount");
		int totalCostAmount = (int) variables.get("totalCostAmount");

		LOGGER.info(
		        "Ticket request type = {}, amount = {}, totalCostAmount = {} was rejected",
		        type, amount, totalCostAmount);

		client.newCompleteCommand(job.getKey()).send();

	}

}