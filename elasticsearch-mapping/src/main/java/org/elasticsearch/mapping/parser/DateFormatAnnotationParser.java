package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.DateFormat;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingBuilder;
import org.elasticsearch.mapping.MappingException;

/**
 * Parse a {@link DateFormat} annotation.
 * 
 * @author luc boutier
 */
public class DateFormatAnnotationParser implements IPropertyAnnotationParser<DateFormat> {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

    public void parseAnnotation(DateFormat annotation, Map<String, Object> fieldDefinition, String pathPrefix,
            Indexable indexable) {
        if (fieldDefinition.get("type") == null) {
            fieldDefinition.put("type", "date");
        } else if (!fieldDefinition.get("type").equals("date")) {
            LOGGER.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        fieldDefinition.put("format", annotation.format());
    }
}