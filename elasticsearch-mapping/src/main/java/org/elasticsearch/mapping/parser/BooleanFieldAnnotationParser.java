package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingBuilder;

/**
 * Parse a {@link BooleanField} annotation.
 * 
 * @author luc boutier
 */
public class BooleanFieldAnnotationParser implements IPropertyAnnotationParser<BooleanField> {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

    public void parseAnnotation(BooleanField annotation, Map<String, Object> fieldDefinition, String pathPrefix,
            Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            LOGGER.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        fieldDefinition.put("type", "boolean");
        fieldDefinition.put("store", annotation.store());
        fieldDefinition.put("index", annotation.index());
        fieldDefinition.put("boost", annotation.boost());
        fieldDefinition.put("include_in_all", annotation.includeInAll());
    }
}