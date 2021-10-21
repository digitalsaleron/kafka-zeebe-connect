/*
 * Class: ZeebeClientJMX
 *
 * Created on Sep 3, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.sps.cdipp.kafkaconnect.infrastructure.configuration;

import java.lang.management.ManagementFactory;

import javax.annotation.PostConstruct;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import io.camunda.zeebe.spring.client.properties.ZeebeClientConfigurationProperties;
import io.camunda.zeebe.spring.client.properties.ZeebeClientProperties;

@Configuration
public class ZeebeClientJMX {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeebeClientJMX.class);

    private ZeebeClientConfigurationProperties clientConfigurationProperties;

    public ZeebeClientJMX(ZeebeClientConfigurationProperties clientConfigurationProperties) {
        super();
        this.clientConfigurationProperties = clientConfigurationProperties;
    }

    @PostConstruct
    private void initialize() {
        registerMbeans();
    }

    private void registerMbeans() {
        try {
            final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            final String zeebeClientName = "vn.sps.cdipp.infrastructure.configuration:category=ZeebeClient,name=zeebeClient";
            final ObjectName objectName = new ObjectName(zeebeClientName);

            StandardMBean standardMBean = new StandardMBean(this.clientConfigurationProperties,
                ZeebeClientProperties.class);

            mbeanServer.registerMBean(standardMBean, objectName);

            LOGGER.info("Register ZeebeClient JMX {}", standardMBean);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException e) {
            LOGGER.error("Error while registering the ZeebeClient JMX. Detail: ", e);
        }
    }
}
