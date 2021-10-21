/*
 * Class: KafkaConsumerManagerJmx
 *
 * Created on Oct 5, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.sps.cdipp.kafkaconnect.application.builder;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName = "vn.sps.cdipp.application.builder:category=KafkaConsumerManagerJmx,name=KafkaConsumerManagerJmx")
public interface KafkaConsumerManagerJmx {
    
    @ManagedOperation(description = "re-initialize a new reponse consumer")
    void reInitializeResponseConsumer(String consumerName);
    
}