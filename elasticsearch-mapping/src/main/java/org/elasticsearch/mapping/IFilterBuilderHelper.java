package org.elasticsearch.mapping;

import org.elasticsearch.index.query.FilterBuilder;

public interface IFilterBuilderHelper {
    /**
     * Get the name of the field mapped by this facet builder.
     * 
     * @return The name of the field.
     */
    String getEsFieldName();

    /**
     * Build a filter.
     * 
     * @param key The key as String.
     * @param value The value as String.
     * @return A filter builder.
     */
    FilterBuilder buildFilter(String key, String value);
}