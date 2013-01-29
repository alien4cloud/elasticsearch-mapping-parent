package org.elasticsearch.mapping.parser;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Parse an annotation on a property.
 * 
 * @author luc boutier
 */
public interface IPropertyAnnotationParser<T extends Annotation> {
	/**
	 * Parse the annotation.
	 * 
	 * @param annotation The annotation to parse (not null).
	 * @param fieldDefinition The map that contains the definition properties for the field.
	 * @param pathPrefix The path prefix for properties that requires a path.
	 * @param field The field concerned by the annotation.
	 * @param propertyDescriptor The field's property descriptor.
	 */
	void parseAnnotation(T annotation, Map<String, Object> fieldDefinition, String pathPrefix, Field field,
			PropertyDescriptor propertyDescriptor);
}