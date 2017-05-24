package org.elasticsearch.mapping.model;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.IndexAnalyserDefinition;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.StringFieldMulti;
import org.elasticsearch.mapping.IndexType;

@ESObject(analyzerDefinitions = @IndexAnalyserDefinition(name = "lowerCaseAnalyser", filters = "lowercase", tokenizer = "keyword"))
public class City {

    @StringFieldMulti(main = @StringField(indexType = IndexType.not_analyzed),
            multiNames = "lower_case",
            multi = @StringField(indexType = IndexType.analyzed, analyser = "lowerCaseAnalyser", includeInAll = false))
    private String city;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}