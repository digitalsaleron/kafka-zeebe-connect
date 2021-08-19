package vn.ds.study.service.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import vn.ds.study.model.JobInfo;
import vn.ds.study.service.JobService;

@Service
public class DefaultJobServiceImpl implements JobService {

	private Map<String, JobInfo> store = new ConcurrentHashMap<>();

	@Override
	public void addJob(JobInfo job) {
		store.put(job.getTicketId(), job);
	}

	@Override
	public JobInfo next() {
		Optional<String> firstJobId = store.keySet().stream().findFirst();
		if (!firstJobId.isPresent()) {
			return null;
		}

		return store.remove(firstJobId.get());
	}

    @Override
    public JobInfo find(String ticketId) {
        return store.remove(ticketId);
    }
}