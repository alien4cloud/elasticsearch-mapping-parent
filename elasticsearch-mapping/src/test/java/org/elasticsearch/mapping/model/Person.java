package org.elasticsearch.mapping.model;

import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import java.util.Map;

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
    @TermFilter
    @StringField(indexType = IndexType.analyzed)
    private String lastname;

    @NestedObject
    private Address address;

    private Address alternateAddress;

    // index this by specifying key and value as fields, do not index the key.
    @MapKeyValue(indexType = IndexType.no)
    private Map<String, Address> addressMap;

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

    public Address getAlternateAddress() {
        return alternateAddress;
    }

    public void setAlternateAddress(Address alternateAddress) {
        this.alternateAddress = alternateAddress;
    }

    public Map<String, Address> getAddressMap() {
        return addressMap;
    }

    public void setAddressMap(Map<String, Address> addressMap) {
        this.addressMap = addressMap;
    }
}
