/*
 * Class: DeploymentNotification
 *
 * Created on Sep 30, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.sps.cdipp.kafkaconnect.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import vn.sps.cdipp.kafkaconnect.model.event.IntermediateEvent;
import vn.sps.cdipp.kafkaconnect.model.event.MessageStartEvent;

public class DeploymentNotification {
    
    @NotNull 
    private String processId;
    
    private Integer version;
    
    private List<MessageStartEvent> messageStartEvents = new ArrayList<>();
    
    private List<IntermediateEvent> intermediateEvents = new ArrayList<>();

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<MessageStartEvent> getMessageStartEvents() {
        return messageStartEvents;
    }

    public void setMessageStartEvents(List<MessageStartEvent> messageStartEvents) {
        this.messageStartEvents = messageStartEvents;
    }

    public List<IntermediateEvent> getIntermediateEvents() {
        return intermediateEvents;
    }

    public void setIntermediateEvents(List<IntermediateEvent> intermediateEvents) {
        this.intermediateEvents = intermediateEvents;
    }
}