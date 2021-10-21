/*
 * Class: MessageStartEvent
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
package vn.sps.cdipp.kafkaconnect.model.event;

public class MessageStartEvent {
    
    private String messageName;

    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageEvent) {
        this.messageName = messageEvent;
    }
    
}