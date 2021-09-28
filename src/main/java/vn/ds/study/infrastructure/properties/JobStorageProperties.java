/*
 * Class: JobCacheProperties
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
package vn.ds.study.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job-storage")
public class JobStorageProperties {
    
    private boolean jobRemovalEnabled;
    
    private boolean exceptionIgnoreEnabled;

    public boolean isJobRemovalEnabled() {
        return jobRemovalEnabled;
    }

    public void setJobRemovalEnabled(boolean jobRemovalEnable) {
        this.jobRemovalEnabled = jobRemovalEnable;
    }

    public boolean isExceptionIgnoreEnabled() {
        return exceptionIgnoreEnabled;
    }

    public void setExceptionIgnoreEnabled(boolean exceptionIgnoreEnabled) {
        this.exceptionIgnoreEnabled = exceptionIgnoreEnabled;
    }
    
}