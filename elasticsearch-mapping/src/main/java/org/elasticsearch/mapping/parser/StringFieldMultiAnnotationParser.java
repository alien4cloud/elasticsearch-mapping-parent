package org.elasticsearch.mapping.parser;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.StringFieldMulti;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.*;

/**
 * Parse a {@link StringField} annotation.
 * 
 * @author luc boutier
 */
public class StringFieldMultiAnnotationParser implements IPropertyAnnotationParser<StringFieldMulti> {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);
    private StringFieldAnnotationParser wrapped = new StringFieldAnnotationParser();

    public void parseAnnotation(StringFieldMulti annotation, Map<String, Object> fieldDefinition, String pathPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            LOGGER.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        StringField mainStringField = annotation.main();
        wrapped.parseAnnotation(mainStringField, fieldDefinition, pathPrefix, indexable);

        Map<String, Object> multiFields = Maps.newHashMap();

        for (int i = 0; i < annotation.multi().length; i++) {
            StringField multi = annotation.multi()[i];
            Map<String, Object> multiFieldDefinition = Maps.newHashMap();
            wrapped.parseAnnotation(multi, multiFieldDefinition, pathPrefix, indexable);
            multiFields.put(annotation.multiNames()[i], multiFieldDefinition);
        }
        fieldDefinition.put("fields", multiFields);
    }
}