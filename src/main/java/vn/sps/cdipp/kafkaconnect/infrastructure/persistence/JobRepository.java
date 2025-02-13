/*
 * Class: JobPersistence
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
package vn.sps.cdipp.kafkaconnect.infrastructure.persistence;

import vn.sps.cdipp.kafkaconnect.model.JobInfo;


public interface JobRepository {

    void addJob(JobInfo jobInfo);
    
    JobInfo getJob(String correlationKey);
    
}