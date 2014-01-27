package org.elasticsearch.mapping.parser;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;

import org.elasticsearch.annotation.NullValue;


/**
 * Parse a {@link NullValue} annotation.
 * 
 * @author luc boutier
 */
public class NullValueAnnotationParser implements IPropertyAnnotationParser<NullValue> {
	public void parseAnnotation(NullValue annotation, Map<String, Object> fieldDefinition, String pathPrefix,
			Field field, PropertyDescriptor propertyDescriptor) {
		fieldDefinition.put("null_value", annotation.nullValue());
	}
}