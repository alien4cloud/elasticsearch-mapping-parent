package org.elasticsearch.mapping.model;

import org.elasticsearch.annotation.*;
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

    @NumberField(index = IndexType.not_analyzed, includeInAll = false)
    private long alienScore = 1;

    public long getAlienScore() {
        return alienScore;
    }

    public void setAlienScore(long alienScore) {
        this.alienScore = alienScore;
    }

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