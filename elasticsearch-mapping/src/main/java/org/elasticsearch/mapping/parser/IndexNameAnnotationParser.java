package org.elasticsearch.mapping.parser;

import java.util.Map;

import org.elasticsearch.annotation.IndexName;
import org.elasticsearch.mapping.Indexable;

/**
 * Parse an {@link IndexName} annotation.
 * 
 * @author luc boutier
 */
public class IndexNameAnnotationParser implements IPropertyAnnotationParser<IndexName> {
    public void parseAnnotation(IndexName annotation, Map<String, Object> fieldDefinition, String pathPrefix,
            Indexable indexable) {
        fieldDefinition.put("index_name", annotation.indexName());
    }
}