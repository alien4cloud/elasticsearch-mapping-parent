package org.elasticsearch.mapping;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.facet.FacetBuilder;

/**
 * Utility that build facet for an object.
 * 
 * @author luc boutier
 */
public interface IFacetBuilderHelper {
    /**
     * Get the name of the field mapped by this facet builder.
     * 
     * @return The name of the field.
     */
    String getEsFieldName();

    /**
     * Build a facet.
     * 
     * @return The facet.
     */
    FacetBuilder buildFacet();

    /**
     * Build a filter that matches the facet type.
     * 
     * @param key The key as String.
     * @param value The value as String.
     * @return A filter builder that matches the facet type.
     */
    FilterBuilder buildAssociatedFilter(String key, String value);
}
