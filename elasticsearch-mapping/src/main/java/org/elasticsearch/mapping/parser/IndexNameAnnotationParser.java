package org.elasticsearch.mapping.parser;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;

import org.elasticsearch.annotation.IndexName;

/**
 * Parse an {@link IndexName} annotation.
 * 
 * @author luc boutier
 */
public class IndexNameAnnotationParser implements IPropertyAnnotationParser<IndexName> {
	public void parseAnnotation(IndexName annotation, Map<String, Object> fieldDefinition, String pathPrefix,
			Field field, PropertyDescriptor propertyDescriptor) {
		fieldDefinition.put("index_name", annotation.indexName());
	}
}