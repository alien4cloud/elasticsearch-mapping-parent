package org.elasticsearch.mapping.model;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

public class Address {
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String city;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
