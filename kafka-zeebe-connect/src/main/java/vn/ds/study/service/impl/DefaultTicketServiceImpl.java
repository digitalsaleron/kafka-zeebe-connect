package vn.ds.study.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import vn.ds.study.model.ResolvedTicketResult;
import vn.ds.study.model.TicketRequest;
import vn.ds.study.service.TicketService;

@Service
public class DefaultTicketServiceImpl implements TicketService {

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

}
