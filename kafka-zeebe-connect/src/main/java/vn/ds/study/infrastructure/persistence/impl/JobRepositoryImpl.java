/*
 * Class: JobPersistenceImpl
 *
 * Created on Sep 1, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.infrastructure.persistence.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.model.JobInfo;

@Repository
public class JobRepositoryImpl implements JobRepository{

	// TODO -- Is it possible that we add high availability for this store?
    private Map<String, JobInfo> store = new ConcurrentHashMap<>();

    @Override
    public void addJob(JobInfo jobInfo) {
        store.put(jobInfo.getCorrelationKey(), jobInfo);
    }

    @Override
    public JobInfo findJob(String correlationKey) {
        return this.store.remove(correlationKey);
    }
}