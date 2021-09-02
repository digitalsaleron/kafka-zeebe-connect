/*
 * Class: ConsumerRepository
 *
 * Created on Sep 2, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.infrastructure.persistence;

public interface ConsumerRepository {
    
    boolean addConsumerIfAbsent(String consumerName);
    
    String removeConsumer(String consumerName);
}
