package vn.ds.study.model;

public class JobInfo {
    
    private String correlationKey;
    
	private long instanceId;

	private long jobId;
	
	private ActivatedJob activatedJob;
	
	public static JobInfo from(String ticketId, long instanceId, long jobId,
	        ActivatedJob activatedJob) {
		return new JobInfo(ticketId, instanceId, jobId, activatedJob);
	}

	private JobInfo(String correlationKey, long instanceId, long jobId, ActivatedJob activatedJob) {
		super();
		this.correlationKey = correlationKey;
		this.instanceId = instanceId;
		this.jobId = jobId;
		this.activatedJob = activatedJob;
	}

	public long getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(long instanceId) {
		this.instanceId = instanceId;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public ActivatedJob getActivatedJob() {
		return activatedJob;
	}

	public void setActivatedJob(ActivatedJob activatedJob) {
		this.activatedJob = activatedJob;
	}

    public String getCorrelationKey() {
        return correlationKey;
    }
}