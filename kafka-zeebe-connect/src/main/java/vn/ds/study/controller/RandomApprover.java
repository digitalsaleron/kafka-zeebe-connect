package vn.ds.study.controller;

import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import vn.ds.study.model.TicketRequest;

@Component
public class RandomApprover {

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(RandomApprover.class);

	@Autowired
	private ZeebeTicketController tickets;
	private Random rd = new Random();

	@Scheduled(fixedDelay = 1000)
	public void run() {

		List<TicketRequest> requests = tickets.get(1);
		if (requests != null && requests.size() > 0) {
			int nextInt = rd.nextInt(1000);
			String approveStatus = nextInt % 2 == 0 ? "true" : "false";
			String ticketId = requests.get(0).getId();

			LOGGER.info("Let's approve ticket {} with status {}", ticketId,
			        approveStatus);

			tickets.approve(ticketId, approveStatus);

		}
	}
}
