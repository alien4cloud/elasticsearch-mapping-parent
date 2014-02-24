package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.DateField;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingException;

/**
 * Parse a {@link DateField} annotation.
 * 
 * @author luc boutier
 */
public class DateFieldAnnotationParser implements IPropertyAnnotationParser<DateField> {
    public void parseAnnotation(DateField annotation, Map<String, Object> fieldDefinition, String pathPrefix,
            Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            throw new MappingException(
                    "A field cannot have more than one Elastic Search type. Parsing StringField on <"
                            + indexable.getDeclaringClassName() + "." + indexable.getName()
                            + "> type is already set to <" + fieldDefinition.get("type") + ">");
        }

        fieldDefinition.put("type", "date");
        fieldDefinition.put("store", annotation.store());
        fieldDefinition.put("index", annotation.index());
        fieldDefinition.put("precision_step", annotation.precisionStep());
        fieldDefinition.put("boost", annotation.boost());
        fieldDefinition.put("include_in_all", annotation.includeInAll());
        fieldDefinition.put("ignore_malformed", annotation.ignoreMalformed());
    }
}