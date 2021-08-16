package vn.ds.study.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.camunda.zeebe.client.ZeebeClient;
import vn.ds.study.model.TicketRequest;

@RestController
@RequestMapping("tickets")
public class ZeebeTicketController {

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(ZeebeTicketController.class);

	@Autowired
	private ZeebeClient client;

	@Autowired
	private ZeebeTicketApprovalHandler ticketStore;

	@GetMapping("/list")
	public List<TicketRequest> list() {
		return ticketStore.getTickets(-1);
	}

	@GetMapping("/get-first/{firstLimit}")
	public List<TicketRequest> get(
	        @PathVariable(name = "firstLimit", required = false) Integer firstLimit) {
		if (firstLimit == null) {
			firstLimit = 1;
		}
		return ticketStore.getTickets(firstLimit);
	}

	@PostMapping("/approve/{ticketId}/{approveStatus}")
	public void approve(
	        @PathVariable(name = "ticketId", required = true) String ticketId,
	        @PathVariable(name = "approveStatus", required = true) String approveStatus) {

		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("ticketId", ticketId);

		ticketStore.waitingRequests.remove(ticketId);
		variables.put("isApproved", approveStatus);

		LOGGER.info("Approve ticket id = {} with approveStatus = {}", ticketId,
		        approveStatus);
		client.newPublishMessageCommand().messageName("approvalMessage")
		        .correlationKey(ticketId).variables(variables).send();
	}

}