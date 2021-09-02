/*
 * Class: ConsumerRepositoryImpl
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
package vn.ds.study.infrastructure.persistence.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import vn.ds.study.infrastructure.persistence.ConsumerRepository;

@Repository
public class ConsumerRepositoryImpl implements ConsumerRepository{

    private Map<String, String> store = new ConcurrentHashMap<>();
    
    @Override
    public void addConsumer(String consumerName) {
        this.store.put(consumerName, consumerName);
    }

    @Override
    public boolean containConsumer(String consumerName) {
        return this.store.containsKey(consumerName);
    }

    @Override
    public synchronized boolean addConsumerIfAbsent(String consumerName) {
        boolean result = this.store.containsKey(consumerName);
        this.store.putIfAbsent(consumerName, consumerName);
        return result;
    }
}