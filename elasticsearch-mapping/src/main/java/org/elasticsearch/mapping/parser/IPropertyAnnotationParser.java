package org.elasticsearch.mapping.parser;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.elasticsearch.mapping.Indexable;

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
     * @param nestedPrefix The nested prefix for properties that are inside a nested object.
     * @param indexable TODO
     */
    void parseAnnotation(T annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix, Indexable indexable);
}