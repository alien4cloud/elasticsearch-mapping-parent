package org.elasticsearch.mapping;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * Adapt a search request builder
 */
public interface ISearchBuilderAdapter {
    /**
     * Adapt a search request builder.
     *
     * @param searchRequestBuilder A search request builder to adapt.
     * @return The query builder with adaptations.
     */
    void adapt(SearchRequestBuilder searchRequestBuilder);
}
