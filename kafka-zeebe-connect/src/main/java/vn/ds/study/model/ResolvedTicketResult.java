package vn.ds.study.model;

public class ResolvedTicketResult {

	private final TicketRequest ticket;
	private boolean isResolved;
	private String resolution;

	private ResolvedTicketResult(TicketRequest ticket, boolean isResolved,
	        String resolution) {
		super();
		this.ticket = ticket;
		this.isResolved = isResolved;
		this.resolution = resolution;
	}

	public static ResolvedTicketResult from(TicketRequest ticket) {
		return new ResolvedTicketResult(ticket, false, null);
	}

	public void setResolved(boolean isResolved) {
		this.isResolved = isResolved;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public TicketRequest getTicket() {
		return ticket;
	}

	public boolean isResolved() {
		return isResolved;
	}

	public String getResolution() {
		return resolution;
	}

}
