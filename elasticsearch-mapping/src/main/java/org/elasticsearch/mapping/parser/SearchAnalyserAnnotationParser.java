package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.SearchAnalyser;
import org.elasticsearch.mapping.Indexable;

/**
 * Parse a {@link SearchAnalyser} annotation.
 * 
 * @author luc boutier
 */
public class SearchAnalyserAnnotationParser implements IPropertyAnnotationParser<SearchAnalyser> {
    public void parseAnnotation(SearchAnalyser annotation, Map<String, Object> fieldDefinition, String pathPrefix,
            Indexable indexable) {
        fieldDefinition.put("search_analyzer", annotation.searchAnalyzer());
    }
}