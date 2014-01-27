package org.elasticsearch.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.facet.FacetBuilder;
import org.springframework.stereotype.Component;

/**
 * Helper class for queries with Elastic Search.
 * 
 * @author luc boutier
 */
@Component
public class QueryHelper {
    @Resource
    private MappingBuilder mappingBuilder;

    public List<FilterBuilder> buildFilters(String className, Map<String, String> filters) {
        List<FilterBuilder> filterBuilders = new ArrayList<FilterBuilder>();

        if (filters == null) {
            return filterBuilders;
        }

        List<IFacetBuilderHelper> facetBuilderHelpers = mappingBuilder.getFacets(className);
        if (facetBuilderHelpers == null) {
            return filterBuilders;
        }

        for (IFacetBuilderHelper facetBuilderHelper : facetBuilderHelpers) {
            String esFieldName = facetBuilderHelper.getEsFieldName();
            if (filters.containsKey(esFieldName)) {
                filterBuilders.add(facetBuilderHelper.buildAssociatedFilter(esFieldName, filters.get(esFieldName)));
            }
        }

        return filterBuilders;
    }

    /**
     * Create a list of facets for the given type.
     * 
     * @param clazz The class for which to create facets.
     * @param filters The set of facets to exclude from the facet creation.
     * @return a {@link List} of {@link AbstractFacetBuilder facet builders}.
     */
    public List<FacetBuilder> buildFacets(Class<?> clazz, Set<String> filters) {
        return buildFacets(clazz.getName(), filters);
    }

    /**
     * Create a list of facets for the given type.
     * 
     * @param className The name of the class for which to create facets.
     * @param filters The set of facets to exclude from the facet creation.
     * @return a {@link List} of {@link AbstractFacetBuilder facet builders}.
     */
    public List<FacetBuilder> buildFacets(String className, Set<String> filters) {
        final List<FacetBuilder> facetBuilders = new ArrayList<FacetBuilder>();

        List<IFacetBuilderHelper> facetBuilderHelpers = mappingBuilder.getFacets(className);
        if (facetBuilderHelpers == null) {
            return facetBuilders;
        }

        for (IFacetBuilderHelper facetBuilderHelper : facetBuilderHelpers) {
            if (filters == null || !filters.contains(facetBuilderHelper.getEsFieldName())) {
                facetBuilders.add(facetBuilderHelper.buildFacet());
            }
        }
        return facetBuilders;
    }
}