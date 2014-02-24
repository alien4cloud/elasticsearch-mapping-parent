package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.DateFormat;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingException;

/**
 * Parse a {@link DateFormat} annotation.
 * 
 * @author luc boutier
 */
public class DateFormatAnnotationParser implements IPropertyAnnotationParser<DateFormat> {
    public void parseAnnotation(DateFormat annotation, Map<String, Object> fieldDefinition, String pathPrefix,
            Indexable indexable) {
        if (fieldDefinition.get("type") == null) {
            fieldDefinition.put("type", "date");
        } else if (!fieldDefinition.get("type").equals("date")) {
            throw new MappingException(
                    "Date format annotation requires a date type to be set for the field current is <"
                            + fieldDefinition.get("type") + ">.");
        }

        fieldDefinition.put("format", annotation.format());
    }
}