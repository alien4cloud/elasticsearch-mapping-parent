package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.DateField;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.IndexType;
import org.elasticsearch.mapping.MappingBuilder;
import org.elasticsearch.mapping.MappingException;
import lombok.extern.slf4j.Slf4j;

/**
 * Parse a {@link DateField} annotation.
 * 
 * @author luc boutier
 */
@Slf4j
public class DateFieldAnnotationParser implements IPropertyAnnotationParser<DateField> {

    public void parseAnnotation(DateField annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            log.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        fieldDefinition.put("type", "date");
        fieldDefinition.put("store", annotation.store());
        //fieldDefinition.put("index", annotation.index());
        fieldDefinition.put("index", annotation.index() == IndexType.no ? "false" : "true");
        //fieldDefinition.put("precision_step", annotation.precisionStep());
        fieldDefinition.put("boost", annotation.boost());
        //fieldDefinition.put("include_in_all", annotation.includeInAll());
        if (annotation.includeInAll()) {
           fieldDefinition.put("copy_to", "all");
        }
        fieldDefinition.put("ignore_malformed", annotation.ignoreMalformed());
    }
}
