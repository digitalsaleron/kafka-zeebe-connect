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

    private Map<String, JobInfo> store = new ConcurrentHashMap<>();

    @Override
    public void addJob(JobInfo jobInfo) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public JobInfo findJob(String correlationKey) {
        // TODO Auto-generated method stub
        return null;
    }
}