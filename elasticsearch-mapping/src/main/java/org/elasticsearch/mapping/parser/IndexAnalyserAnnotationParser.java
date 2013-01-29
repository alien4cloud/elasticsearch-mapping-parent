package org.elasticsearch.mapping.parser;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;

import org.elasticsearch.annotation.IndexAnalyser;

/**
 * Parse an {@link IndexAnalyser} annotation.
 * 
 * @author luc boutier
 */
public class IndexAnalyserAnnotationParser implements IPropertyAnnotationParser<IndexAnalyser> {
	public void parseAnnotation(IndexAnalyser annotation, Map<String, Object> fieldDefinition, String pathPrefix,
			Field field, PropertyDescriptor propertyDescriptor) {
		fieldDefinition.put("index_analyzer", annotation.indexAnalyzer());
	}
}