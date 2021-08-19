package vn.ds.study.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import vn.ds.study.model.ResolvedTicketResult;
import vn.ds.study.model.TicketRequest;
import vn.ds.study.service.TicketService;

@Service
public class DefaultTicketServiceImpl implements TicketService {

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(DefaultTicketServiceImpl.class);

	@Value("${approval.rule.defaultAmount:5000}")
	private long defaultApprovedAmount;

	@Override
	public boolean approve(TicketRequest ticket) {
		return ticket.getTotalCostAmount() <= defaultApprovedAmount;
	}

	@Override
	public ResolvedTicketResult resolve(TicketRequest ticket) {
		ResolvedTicketResult res = ResolvedTicketResult.from(ticket);

		res.setResolved(true);
		res.setResolution("Go ahead and purchase the required facilities");

		return res;
	}

	@Override
	public boolean validate(TicketRequest request) {
		LOGGER.info(
		        "Validate ticket request type = {}, amount = {}, totalCostAmount = {}",
		        request.getType(), request.getAmount(),
		        request.getTotalCostAmount());

		return request.getAmount() > 0 && request.getTotalCostAmount() > 0;
	}

}
