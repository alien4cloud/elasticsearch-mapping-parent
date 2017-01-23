package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.NullValue;
import org.elasticsearch.mapping.Indexable;

/**
 * Parse a {@link NullValue} annotation.
 * 
 * @author luc boutier
 */
public class NullValueAnnotationParser implements IPropertyAnnotationParser<NullValue> {
    public void parseAnnotation(NullValue annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix,
            Indexable indexable) {
        fieldDefinition.put("null_value", annotation.nullValue());
    }
}