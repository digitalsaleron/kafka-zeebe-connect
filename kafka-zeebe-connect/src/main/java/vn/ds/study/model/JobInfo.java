package vn.ds.study.model;

import io.camunda.zeebe.client.api.response.ActivatedJob;

public class JobInfo {
	private long instanceId;
	private long jobId;
	private ActivatedJob activatedJob;

	public static JobInfo from(long instanceId, long jobId,
	        ActivatedJob activatedJob) {
		return new JobInfo(instanceId, jobId, activatedJob);
	}

	private JobInfo(long instanceId, long jobId, ActivatedJob activatedJob) {
		super();
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

}
