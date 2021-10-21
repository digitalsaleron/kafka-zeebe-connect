/*
 * Class: ActivatedJob
 *
 * Created on Sep 9, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.sps.cdipp.kafkaconnect.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.camunda.zeebe.client.api.JsonMapper;
import io.camunda.zeebe.client.impl.ZeebeObjectMapper;

public class ActivatedJob {

    @JsonIgnore
    private JsonMapper jsonMapper = new ZeebeObjectMapper();

    private long key;

    private String type;

    private Map<String, String> customHeaders;

    private long processInstanceKey;

    private String bpmnProcessId;

    private int processDefinitionVersion;

    private long processDefinitionKey;

    private String elementId;

    private long elementInstanceKey;

    private String worker;

    private int retries;

    private long deadline;

    private String variables;

    public long getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    public long getProcessInstanceKey() {
        return processInstanceKey;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public int getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public long getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getElementId() {
        return elementId;
    }

    public long getElementInstanceKey() {
        return elementInstanceKey;
    }

    public String getWorker() {
        return worker;
    }

    public int getRetries() {
        return retries;
    }

    public long getDeadline() {
        return deadline;
    }

    public String getVariables() {
        return variables;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }

    public void setProcessInstanceKey(long processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public void setProcessDefinitionVersion(int processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public void setProcessDefinitionKey(long processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public void setElementInstanceKey(long elementInstanceKey) {
        this.elementInstanceKey = elementInstanceKey;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }
    
    public Map<String, Object> getVariablesAsMap() {
        return jsonMapper.fromJsonAsMap(variables);
    }
}