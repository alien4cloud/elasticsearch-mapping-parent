package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.DateFormat;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingBuilder;
import org.elasticsearch.mapping.MappingException;
import lombok.extern.slf4j.Slf4j;

/**
 * Parse a {@link DateFormat} annotation.
 * 
 * @author luc boutier
 */
@Slf4j
public class DateFormatAnnotationParser implements IPropertyAnnotationParser<DateFormat> {

    public void parseAnnotation(DateFormat annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix,
            Indexable indexable) {
        if (fieldDefinition.get("type") == null) {
            fieldDefinition.put("type", "date");
        } else if (!fieldDefinition.get("type").equals("date")) {
            log.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        fieldDefinition.put("format", annotation.format());
    }
}
