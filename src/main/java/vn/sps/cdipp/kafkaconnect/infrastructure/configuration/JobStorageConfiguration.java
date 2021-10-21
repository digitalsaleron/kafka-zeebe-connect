/*
 * Class: JobCaccheConfiguration
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
package vn.sps.cdipp.kafkaconnect.infrastructure.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import vn.sps.cdipp.kafkaconnect.infrastructure.properties.JobStorageProperties;

@Configuration
@EnableConfigurationProperties(value = {JobStorageProperties.class})
public class JobStorageConfiguration {
    
}
