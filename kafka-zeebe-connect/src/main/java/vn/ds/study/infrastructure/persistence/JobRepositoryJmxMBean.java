/*
 * Class: JobRepositoryJmx
 *
 * Created on Sep 10, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.infrastructure.persistence;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName = "vn.sps.cdipp.infrastructure.persistence:category=JobRepository,name=jobRepository")
public interface JobRepositoryJmxMBean {

    @ManagedOperation(description = "Returns the number of entries in this repository")
    long size();
    
    @ManagedOperation(description = "Re-load the job storage")
    void reload();

}