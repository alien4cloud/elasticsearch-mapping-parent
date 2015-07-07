package org.elasticsearch.mapping.model;

import org.elasticsearch.annotation.ESAll;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

/**
 * 
 * @author luc boutier
 */
@ESObject
@ESAll(analyser = "simple")
public class Person {
    @Id
    private String id;
    @StringField(indexType = IndexType.no, includeInAll = false)
    private String firstname;
    @StringField(indexType = IndexType.analyzed)
    private String lastname;

    @NestedObject
    private Address address;

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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}