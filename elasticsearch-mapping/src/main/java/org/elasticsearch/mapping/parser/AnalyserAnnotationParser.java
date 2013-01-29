package org.elasticsearch.mapping.parser;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;

import org.elasticsearch.annotation.Analyser;

/**
 * Parse an {@link Analyser} annotation.
 * 
 * @author luc boutier
 */
public class AnalyserAnnotationParser implements IPropertyAnnotationParser<Analyser> {
	public void parseAnnotation(Analyser annotation, Map<String, Object> fieldDefinition, String pathPrefix,
			Field field, PropertyDescriptor propertyDescriptor) {
		fieldDefinition.put("analyser", annotation.analyzer());
	}
}