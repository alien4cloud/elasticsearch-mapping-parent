package org.elasticsearch.mapping.parser;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.StringFieldMulti;
import com.google.common.collect.Maps;
import org.elasticsearch.mapping.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Parse a {@link StringField} annotation.
 * 
 * @author luc boutier
 */
@Slf4j
public class StringFieldMultiAnnotationParser implements IPropertyAnnotationParser<StringFieldMulti> {
    private StringFieldAnnotationParser wrapped = new StringFieldAnnotationParser();

    public void parseAnnotation(StringFieldMulti annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            log.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        StringField mainStringField = annotation.main();
        wrapped.parseAnnotation(mainStringField, fieldDefinition, pathPrefix, nestedPrefix, indexable);

        Map<String, Object> multiFields = Maps.newHashMap();

        for (int i = 0; i < annotation.multi().length; i++) {
            StringField multi = annotation.multi()[i];
            Map<String, Object> multiFieldDefinition = Maps.newHashMap();
            wrapped.parseAnnotation(multi, multiFieldDefinition, pathPrefix, nestedPrefix, indexable);
            multiFields.put(annotation.multiNames()[i], multiFieldDefinition);
        }
        fieldDefinition.put("fields", multiFields);
    }
}
