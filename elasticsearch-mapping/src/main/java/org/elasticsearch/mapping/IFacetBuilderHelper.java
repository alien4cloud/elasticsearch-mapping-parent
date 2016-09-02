package org.elasticsearch.mapping;

import org.elasticsearch.search.aggregations.AggregationBuilder;

/**
 * Utility that build facet for an object.
 * 
 * @author luc boutier
 */
public interface IFacetBuilderHelper extends IFilterBuilderHelper {
    /**
     * Build a facet.
     * 
     * @return The facet.
     */
    AggregationBuilder buildFacet();
}