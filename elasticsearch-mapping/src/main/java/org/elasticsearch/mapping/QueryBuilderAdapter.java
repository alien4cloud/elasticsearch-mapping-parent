package org.elasticsearch.mapping;

import org.elasticsearch.index.query.QueryBuilder;

/**
 * Interface used to allow modification of the Query Builder when using QueryHelper to generate the search query.
 */
public interface QueryBuilderAdapter {
    /**
     * Adapt a query.
     * 
     * @param queryBuilder A query builder to adapt before adding filters, sort and other post-processing elements to the query.
     * @return The query builder with adaptations.
     */
    QueryBuilder adapt(QueryBuilder queryBuilder);
}
