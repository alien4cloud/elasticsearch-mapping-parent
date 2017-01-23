package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.IndexAnalyser;
import org.elasticsearch.mapping.Indexable;

/**
 * Parse an {@link IndexAnalyser} annotation.
 * 
 * @author luc boutier
 */
public class IndexAnalyserAnnotationParser implements IPropertyAnnotationParser<IndexAnalyser> {
    public void parseAnnotation(IndexAnalyser annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix,
            Indexable indexable) {
        fieldDefinition.put("index_analyzer", annotation.indexAnalyzer());
    }
}