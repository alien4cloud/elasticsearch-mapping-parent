package org.elasticsearch.mapping;

import java.util.List;

import org.elasticsearch.search.aggregations.AggregationBuilder;

/**
 * Utility that build facet for an object.
 * 
 * @author luc boutier
 */
public interface IFacetBuilderHelper extends IFilterBuilderHelper {
    /**
     * Build facets.
     * 
     * @return The facets.
     */
    List<AggregationBuilder> buildFacets();
}