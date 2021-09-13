/*
 * Class: TopicProperties
 *
 * Created on Sep 7, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.infrastructure.properties;

public class KafkaTopicProperties {

    private String suffix;
    
    private String prefix;
    
    private String name;
    
    private boolean prefixIsPattern;

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPrefixIsPattern() {
        return prefixIsPattern;
    }

    public void setPrefixIsPattern(boolean prefixIsPattern) {
        this.prefixIsPattern = prefixIsPattern;
    }
}