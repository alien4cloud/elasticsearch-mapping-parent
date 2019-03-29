package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.NumberField;
import org.elasticsearch.mapping.Indexable;
import org.elasticsearch.mapping.MappingBuilder;
import org.elasticsearch.mapping.MappingException;
import lombok.extern.slf4j.Slf4j;

/**
 * Parse a {@link NumberField} annotation.
 * 
 * @author luc boutier
 */
@Slf4j
public class NumberFieldAnnotationParser implements IPropertyAnnotationParser<NumberField> {

    public void parseAnnotation(NumberField annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix,
            Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            log.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        String type = getESNumberType(indexable);
        if (type == null) {
            String error = "Field <"
                    + indexable.getName()
                    + "> has a Number annotation but type is <"
                    + indexable.getType().getName()
                    + "> and should be one of Float, float, Double, double, Integer, int, Long, long, Short, short, Byte or byte and therefore is not supported.";
            throw new MappingException(error);
        }

        fieldDefinition.put("type", type);
        fieldDefinition.put("store", annotation.store());
        fieldDefinition.put("index", annotation.index());
        fieldDefinition.put("precision_step", annotation.precisionStep());
        fieldDefinition.put("boost", annotation.boost());
        fieldDefinition.put("include_in_all", annotation.includeInAll());
        fieldDefinition.put("ignore_malformed", annotation.ignoreMalformed());
    }

    private String getESNumberType(Indexable indexable) {
        Class<?> fieldType = indexable.getType();
        while (fieldType.isArray()) {
            fieldType = fieldType.getComponentType();
        }

        if (Float.TYPE.equals(fieldType) || Float.class.equals(fieldType)) {
            return "float";
        } else if (Double.TYPE.equals(fieldType) || Double.class.equals(fieldType)) {
            return "double";
        } else if (Integer.TYPE.equals(fieldType) || Integer.class.equals(fieldType)) {
            return "integer";
        } else if (Long.TYPE.equals(fieldType) || Long.class.equals(fieldType)) {
            return "long";
        } else if (Short.TYPE.equals(fieldType) || Short.class.equals(fieldType)) {
            return "short";
        } else if (Byte.TYPE.equals(fieldType) || Byte.class.equals(fieldType)) {
            return "byte";
        }
        return null;
    }
}
