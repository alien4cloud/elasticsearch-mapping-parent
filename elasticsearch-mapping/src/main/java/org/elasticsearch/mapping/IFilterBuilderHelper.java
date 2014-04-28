package org.elasticsearch.mapping;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;

public interface IFilterBuilderHelper {

    /**
     * Should the filter created by this filter builder be wrapped in a Nested filter.
     * 
     * @return True if the filter should be processed as a nested filter, false if not.
     */
    boolean isNested();

    /**
     * Get the path of the nested query for the filter.
     * 
     * @return The path of the nested query field for the filter.
     */
    String getNestedPath();

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
    FilterBuilder buildFilter(String key, String... value);

    /**
     * Build a query builder.
     * 
     * @param key The key as String.
     * @param value The value as String.
     * @return A query builder.
     */
    QueryBuilder buildQuery(String key, String[] value);
}