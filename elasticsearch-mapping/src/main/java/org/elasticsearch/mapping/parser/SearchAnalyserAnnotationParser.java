package org.elasticsearch.mapping.parser;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;

import org.elasticsearch.annotation.SearchAnalyser;

/**
 * Parse a {@link SearchAnalyser} annotation.
 * 
 * @author luc boutier
 */
public class SearchAnalyserAnnotationParser implements IPropertyAnnotationParser<SearchAnalyser> {
	public void parseAnnotation(SearchAnalyser annotation, Map<String, Object> fieldDefinition, String pathPrefix,
			Field field, PropertyDescriptor propertyDescriptor) {
		fieldDefinition.put("search_analyzer", annotation.searchAnalyzer());
	}
}