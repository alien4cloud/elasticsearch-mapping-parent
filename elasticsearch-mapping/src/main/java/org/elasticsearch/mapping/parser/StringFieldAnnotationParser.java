package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingException;

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
        fieldDefinition.put("term_vector", annotation.termVector());
        fieldDefinition.put("boost", annotation.boost());
        fieldDefinition.put("omit_norms", annotation.omitNorms());
        fieldDefinition.put("omit_term_freq_and_positions", annotation.omitTermFreqAndPositions());
        fieldDefinition.put("include_in_all", annotation.includeInAll());
        if (annotation.ignoreAbove() > 0) {
            fieldDefinition.put("ignore_above", annotation.ignoreAbove());
        }
    }
}