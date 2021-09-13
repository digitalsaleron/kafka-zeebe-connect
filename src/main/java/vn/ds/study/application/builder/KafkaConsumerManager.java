/*
 * Class: KafkaConsumerManager
 *
 * Created on Sep 6, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.application.builder;

public interface KafkaConsumerManager {

    boolean findAndAddConsumerIfAbsent(String consumerName);

    String removeConsumer(String consumerName);
    
}