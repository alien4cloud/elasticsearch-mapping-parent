package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingException;

/**
 * Parse a {@link BooleanField} annotation.
 * 
 * @author luc boutier
 */
public class BooleanFieldAnnotationParser implements IPropertyAnnotationParser<BooleanField> {
    public void parseAnnotation(BooleanField annotation, Map<String, Object> fieldDefinition, String pathPrefix,
            Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            throw new MappingException(
                    "A field cannot have more than one Elastic Search type. Parsing BooleanField on <"
                            + indexable.getDeclaringClassName() + "." + indexable.getName()
                            + "> type is already set to <" + fieldDefinition.get("type") + ">");
        }

        fieldDefinition.put("type", "boolean");
        fieldDefinition.put("store", annotation.store());
        fieldDefinition.put("index", annotation.index());
        fieldDefinition.put("boost", annotation.boost());
        fieldDefinition.put("include_in_all", annotation.includeInAll());
    }
}