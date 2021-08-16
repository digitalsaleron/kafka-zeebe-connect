package vn.ds.study.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;

@Component
public class ZeebeTicketValidationHandler {

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(ZeebeTicketValidationHandler.class);

	@ZeebeWorker(type = "validatingTickets")
	public void handleJobFoo(final JobClient client, final ActivatedJob job) {

		Map<String, Object> variables = job.getVariablesAsMap();
		variables.put("isValid", false);

		String type = (String) variables.get("type");
		int amount = (int) variables.get("amount");
		int totalCostAmount = (int) variables.get("totalCostAmount");

		LOGGER.info(
		        "Validate ticket request type = {}, amount = {}, totalCostAmount = {}",
		        type, amount, totalCostAmount);
		variables.put("isValid", amount > 0 && totalCostAmount > 0);

		client.newCompleteCommand(job.getKey()).variables(variables).send();

	}

}