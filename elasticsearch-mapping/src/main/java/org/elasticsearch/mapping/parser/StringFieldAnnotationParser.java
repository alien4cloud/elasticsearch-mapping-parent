package org.elasticsearch.mapping.parser;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexOptions;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingException;
import org.elasticsearch.mapping.NormEnabled;
import org.elasticsearch.mapping.NormLoading;

/**
 * Parse a {@link StringField} annotation.
 * 
 * @author luc boutier
 */
public class StringFieldAnnotationParser implements IPropertyAnnotationParser<StringField> {
    public void parseAnnotation(StringField annotation, Map<String, Object> fieldDefinition, String pathPrefix,
            Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            throw new MappingException(
                    "A field cannot have more than one Elastic Search type. Parsing StringField on <"
                            + indexable.getDeclaringClassName() + "." + indexable.getName()
                            + "> type is already set to <" + fieldDefinition.get("type") + ">");
        }

        fieldDefinition.put("type", "string");
        fieldDefinition.put("store", annotation.store());
        fieldDefinition.put("index", annotation.indexType());
        // TODO doc_values
        fieldDefinition.put("term_vector", annotation.termVector());
        fieldDefinition.put("boost", annotation.boost());
        if (!StringField.NULL_VALUE_NOT_SPECIFIED.equals(annotation.nullValue())) {
            fieldDefinition.put("null_value", annotation.nullValue());
        }
        if (!NormEnabled.DEFAULT.equals(annotation.normsEnabled())) {
            Map<String, Object> norms = new HashMap<String, Object>();
            norms.put("enabled", annotation.normsEnabled().name().toLowerCase());
            if (!NormLoading.DEFAULT.equals(annotation.normsLoading())) {
                norms.put("loading", annotation.normsLoading());
            }
            fieldDefinition.put("norms", norms);
        }
        if (!IndexOptions.DEFAULT.equals(annotation.indexOptions())) {
            fieldDefinition.put("index_options", annotation.indexOptions());
        }
        if (!annotation.analyser().isEmpty()) {
            fieldDefinition.put("analyzer", annotation.analyser());
        }
        if (!annotation.indexAnalyzer().isEmpty()) {
            fieldDefinition.put("index_analyzer", annotation.indexAnalyzer());
        }
        if (!annotation.searchAnalyzer().isEmpty()) {
            fieldDefinition.put("search_analyzer", annotation.searchAnalyzer());
        }

        fieldDefinition.put("include_in_all", annotation.includeInAll());
        if (annotation.ignoreAbove() > 0) {
            fieldDefinition.put("ignore_above", annotation.ignoreAbove());
        }
        
        annotation.positionOffsetGap();
    }
}