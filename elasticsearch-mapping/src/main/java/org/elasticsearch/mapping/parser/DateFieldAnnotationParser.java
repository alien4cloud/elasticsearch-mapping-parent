package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.DateField;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingBuilder;
import org.elasticsearch.mapping.MappingException;

/**
 * Parse a {@link DateField} annotation.
 * 
 * @author luc boutier
 */
public class DateFieldAnnotationParser implements IPropertyAnnotationParser<DateField> {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

    public void parseAnnotation(DateField annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            LOGGER.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
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