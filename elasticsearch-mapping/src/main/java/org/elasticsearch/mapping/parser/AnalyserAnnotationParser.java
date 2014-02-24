package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.Analyser;
import org.elasticsearch.mapping.Indexable;

/**
 * Parse an {@link Analyser} annotation.
 * 
 * @author luc boutier
 */
public class AnalyserAnnotationParser implements IPropertyAnnotationParser<Analyser> {
    public void parseAnnotation(Analyser annotation, Map<String, Object> fieldDefinition, String pathPrefix, Indexable indexable) {
        fieldDefinition.put("analyser", annotation.analyzer());
    }
}