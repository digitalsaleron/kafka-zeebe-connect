package vn.ds.study.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import vn.ds.study.model.JobInfo;
import vn.ds.study.service.JobService;

@Service
public class DefaultJobServiceImpl implements JobService {

	private Map<Long, JobInfo> store = new HashMap<>();

	@Override
	public void addJob(JobInfo job) {
		store.put(job.getJobId(), job);
	}

	@Override
	public JobInfo next() {
		Optional<Long> firstJobId = store.keySet().stream().findFirst();
		if (!firstJobId.isPresent()) {
			return null;
		}

		return store.remove(firstJobId.get());
	}

}
