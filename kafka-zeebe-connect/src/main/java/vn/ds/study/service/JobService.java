package vn.ds.study.service;

import vn.ds.study.model.JobInfo;

public interface JobService {

	void addJob(JobInfo job);

	JobInfo next();
}
