/*
 * Class: ConsumerRestoreEvent
 *
 * Created on Sep 13, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.model.event;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

import vn.ds.study.infrastructure.persistence.JobRepository;
import vn.ds.study.model.JobInfo;

public class ConsumerRecoveryEvent extends ApplicationEvent{

    private static final long serialVersionUID = -4579193099797750080L;
    
    private Map<String, JobInfo> jobIntances;
    
    private JobRepository jobRepository;
    
    public ConsumerRecoveryEvent(Object source, Map<String, JobInfo> jobIntances) {
        super(source);
        this.jobIntances = jobIntances;
        this.jobRepository = (JobRepository) source;
    }

    public Map<String, JobInfo> getJobIntances() {
        return jobIntances;
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }

}