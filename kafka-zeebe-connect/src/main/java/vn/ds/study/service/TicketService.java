package vn.ds.study.service;

import vn.ds.study.model.ResolvedTicketResult;
import vn.ds.study.model.TicketRequest;

public interface TicketService {

	boolean approve(TicketRequest ticket);

	ResolvedTicketResult resolve(TicketRequest ticket);

	boolean validate(TicketRequest request);

}
