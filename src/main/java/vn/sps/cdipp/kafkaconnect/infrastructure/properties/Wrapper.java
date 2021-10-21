/*
 * Class: Wrapper
 *
 * Created on Sep 29, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.sps.cdipp.kafkaconnect.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("wrapper")
public class Wrapper {

    private String responseWrapperKey;

    public String getResponseWrapperKey() {
        return responseWrapperKey;
    }

    public void setResponseWrapperKey(String responseWrapperKey) {
        this.responseWrapperKey = responseWrapperKey;
    }
    
}