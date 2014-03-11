package org.elasticsearch.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Helper class for queries with Elastic Search.
 * 
 * @author luc boutier
 */
@Component
public class QueryHelper {
    private static final ESLogger LOGGER = Loggers.getLogger(QueryHelper.class);

    @Resource
    private MappingBuilder mappingBuilder;
    @Resource
    private ElasticSearchClient esClient;

    private int maxExpansions;

    /**
     * 
     * @param clazz The class that we want to query.
     * @param fetchContext The context to apply to the
     * @param searchQuery The search text if any, can be null if the request doesn't include a search text.
     * @param filters The set of filters for the request (field/value pairs).
     * @param from The start index of the search (for pagination).
     * @param size The maximum number of elements to return.
     * @param enableFacets Flag to know if we should include facets in the search request.
     * @return A {@link SearchResponse} object with the results.
     */
    public SearchResponse doSearch(Class<?> clazz, String[] indexes, String fetchContext, String searchQuery, Map<String, String> filters, int from, int size,
            boolean enableFacets) {
        SearchRequestBuilder searchRequestBuilder = esClient.getClient().prepareSearch(indexes);

        QueryBuilder queryBuilder;
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            queryBuilder = QueryBuilders.matchAllQuery();
        } else {
            queryBuilder = QueryBuilders.matchPhrasePrefixQuery("_all", searchQuery).maxExpansions(this.maxExpansions);
        }

        searchRequestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH).setQuery(queryBuilder).setSize(size).setFrom(from);

        if (clazz != null) {
            searchRequestBuilder.setTypes(MappingBuilder.indexTypeFromClass(clazz));
            // set filters
            addFilters(searchRequestBuilder, clazz, filters, enableFacets);

            if (fetchContext != null) {
                // get the fetch context for the given type and apply it to the search
                SourceFetchContext sourceFetchContext = mappingBuilder.getFetchSource(clazz.getName(), fetchContext);
                if (sourceFetchContext != null) {
                    String[] includes = sourceFetchContext.getIncludes().isEmpty() ? null : sourceFetchContext.getIncludes().toArray(
                            new String[sourceFetchContext.getIncludes().size()]);
                    String[] excludes = sourceFetchContext.getExcludes().isEmpty() ? null : sourceFetchContext.getExcludes().toArray(
                            new String[sourceFetchContext.getExcludes().size()]);
                    searchRequestBuilder.setFetchSource(includes, excludes);
                } else {
                    LOGGER.warn("Unable to find fetch context <" + fetchContext + "> for class <" + clazz.getName() + ">. It will be ignored.");
                }
            }
        }

        searchRequestBuilder.addSort(SortBuilders.scoreSort());

        return searchRequestBuilder.execute().actionGet();
    }

    private void addFilters(SearchRequestBuilder searchRequestBuilder, Class<?> clazz, Map<String, String> filters, boolean enableFacets) {
        final List<FilterBuilder> esFilters = buildFilters(clazz.getName(), filters);
        FilterBuilder filter = null;
        if (esFilters.size() > 0) {
            if (esFilters.size() == 1) {
                filter = esFilters.get(0);
            } else {
                filter = FilterBuilders.andFilter(esFilters.toArray(new FilterBuilder[esFilters.size()]));
            }
            searchRequestBuilder.setPostFilter(filter);
        }
        if (enableFacets) {
            if (filters == null) {
                addFacets(new HashMap<String, String>(), clazz.getName(), searchRequestBuilder, filter);
            } else {
                addFacets(filters, clazz.getName(), searchRequestBuilder, filter);
            }
        }
    }

    private void addFacets(Map<String, String> filters, String className, SearchRequestBuilder searchRequestBuilder,
            FilterBuilder filter) {
        final List<FacetBuilder> facets = buildFacets(className, filters.keySet());
        for (final FacetBuilder facet : facets) {
            if (filter != null) {
                facet.facetFilter(filter);
            }
            searchRequestBuilder.addFacet(facet);
        }
    }

    public List<FilterBuilder> buildFilters(String className, Map<String, String> filters) {
        List<FilterBuilder> filterBuilders = new ArrayList<FilterBuilder>();

        if (filters == null) {
            return filterBuilders;
        }
        
        List<IFilterBuilderHelper> filterBuilderHelpers = mappingBuilder.getFilters(className);
        if (filterBuilderHelpers == null) {
            return filterBuilders;
        }

        for (IFilterBuilderHelper filterBuilderHelper : filterBuilderHelpers) {
            String esFieldName = filterBuilderHelper.getEsFieldName();
            if (filters.containsKey(esFieldName)) {
                filterBuilders.add(filterBuilderHelper.buildFilter(esFieldName, filters.get(esFieldName)));
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

    @Value("#{elasticsearchConfig['elasticSearch.prefix_max_expansions']}")
    public void setMaxExpansions(final int maxExpansions) {
        this.maxExpansions = maxExpansions;
    }
}