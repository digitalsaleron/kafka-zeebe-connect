package vn.ds.study.model;

public class TicketRequest {
	private String id;
	private int amount;
	private int totalCostAmount;
	private boolean isValid;
	private boolean isApproved;
	private String type;

	public String getType() {
		return type;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public boolean isApproved() {
		return isApproved;
	}

	public void setApproved(boolean isApproved) {
		this.isApproved = isApproved;
	}

	private TicketRequest(String id, String type, int amount,
	        int totalCostAmount) {
		super();
		this.id = id;
		this.type = type;
		this.amount = amount;
		this.totalCostAmount = totalCostAmount;
	}

	public static TicketRequest from(String id, String type, int amount,
	        int totalCostAmount) {
		return new TicketRequest(id, type, amount, totalCostAmount);
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public int getTotalCostAmount() {
		return totalCostAmount;
	}

	public void setTotalCostAmount(int totalCostAmount) {
		this.totalCostAmount = totalCostAmount;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
