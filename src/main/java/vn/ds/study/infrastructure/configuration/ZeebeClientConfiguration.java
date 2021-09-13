/*
 * Class: ZeebeClientConfiguration
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
package vn.ds.study.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.impl.ZeebeClientBuilderImpl;

@Configuration
public class ZeebeClientConfiguration {
    
    private ZeebeClientBuilderImpl zeebeClientBuilder;

    public ZeebeClientConfiguration(ZeebeClientBuilderImpl zeebeClientBuilder) {
        super();
        this.zeebeClientBuilder = zeebeClientBuilder;
    }
    
    @Primary
    @Bean("zeebeClient")
    ZeebeClient getZeebeClient() {
        return zeebeClientBuilder.build();
    }
}