package org.elasticsearch.mapping.model;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

/**
 * 
 * @author luc boutier
 */
@ESObject
public class Person {
    @Id
    private String id;
    @StringField(indexType = IndexType.no, includeInAll = false)
    private String firstname;
    @StringField(indexType = IndexType.analyzed)
    private String lastname;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }
}