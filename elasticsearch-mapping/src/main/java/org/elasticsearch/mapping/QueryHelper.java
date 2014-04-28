package org.elasticsearch.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
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
     * Create a {@link SearchQueryHelperBuilder} to prepare a query on elastic search.
     * 
     * @param indices The indices for which to create a search request.
     * @return a {@link SearchQueryHelperBuilder} instance.
     */
    public SearchQueryHelperBuilder buildSearchQuery(String... indices) {
        return buildSearchQuery(indices, null);
    }

    /**
     * Create a search builder for the given indices.
     * 
     * @param indices The indices for which to create a search request.
     * @param searchQuery The search text if any, can be null if the request doesn't include a search text.
     * @return a {@link SearchQueryHelperBuilder} instance.
     */
    public SearchQueryHelperBuilder buildSearchQuery(String[] indices, String searchQuery) {
        return new SearchQueryHelperBuilder(indices, searchQuery);
    }

    /**
     * Create a suggest query based search builder for the given indices.
     * 
     * @param indices The indices for which to create a search request.
     * @param searchPrefix The value of the current prefix for suggestion.
     * @param suggestFieldPath The path to the field for which to manage suggestion.
     * @return a {@link SearchQueryHelperBuilder} instance.
     */
    public SearchQueryHelperBuilder buildSearchSuggestQuery(String[] indices, String searchPrefix, String suggestFieldPath) {
        return new SearchQueryHelperBuilder(indices, searchPrefix, suggestFieldPath);
    }

    /**
     * Create a count builder to prepare a query on elastic search.
     * 
     * @param indices The indices for which to create a count request.
     * @return a {@link CountQueryHelperBuilder} instance.
     */
    public CountQueryHelperBuilder buildCountQuery(String... indices) {
        return buildCountQuery(indices, null);
    }

    /**
     * Create a count builder for the given indices.
     * 
     * @param indices The indices for which to create a count request.
     * @param searchQuery The search text if any, can be null if the request doesn't include a search text.
     * @return a {@link CountQueryHelperBuilder} instance.
     */
    public CountQueryHelperBuilder buildCountQuery(String[] indices, String searchQuery) {
        return new CountQueryHelperBuilder(indices, searchQuery);
    }

    /**
     * Create a suggest query based count builder for the given indices.
     * 
     * @param indices The indices for which to create a search request.
     * @param searchPrefix The value of the current prefix for suggestion.
     * @param suggestFieldPath The path to the field for which to manage suggestion.
     * @return a {@link CountQueryHelperBuilder} instance.
     */
    public CountQueryHelperBuilder buildCountSuggestQuery(String[] indices, String searchPrefix, String suggestFieldPath) {
        return new CountQueryHelperBuilder(indices, searchPrefix, suggestFieldPath);
    }

    @Value("#{elasticsearchConfig['elasticSearch.prefix_max_expansions']}")
    public void setMaxExpansions(final int maxExpansions) {
        this.maxExpansions = maxExpansions;
    }

    /**
     * Builder utility for search requests.
     */
    private abstract class QueryHelperBuilder<T> {
        protected final String prefixField;
        protected final String[] indices;
        protected final QueryBuilder queryBuilder;
        protected Class<?>[] classes;
        protected Map<String, String[]> filters;
        protected Map<String, FilterValuesStrategy> filterStrategies = Maps.newHashMap();

        private QueryHelperBuilder(String[] indices, String searchQuery) {
            this.indices = indices;
            this.prefixField = null;
            QueryBuilder queryBuilder;
            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                queryBuilder = QueryBuilders.matchAllQuery();
            } else {
                queryBuilder = QueryBuilders.matchPhrasePrefixQuery("_all", searchQuery).maxExpansions(maxExpansions);
            }
            this.queryBuilder = queryBuilder;
        }

        private QueryHelperBuilder(String[] indices, String searchPrefix, String prefixField) {
            this.indices = indices;
            this.prefixField = prefixField;
            QueryBuilder queryBuilder;
            if (searchPrefix == null || searchPrefix.trim().isEmpty()) {
                queryBuilder = QueryBuilders.matchAllQuery();
            } else {
                queryBuilder = QueryBuilders.prefixQuery(prefixField, searchPrefix);
            }
            this.queryBuilder = queryBuilder;
        }

        /**
         * Specify types to query.
         * 
         * @param classes The types to query.
         * @return this
         */
        @SuppressWarnings("unchecked")
        public T types(Class<?>... classes) {
            this.classes = classes;
            return (T) this;
        }

        /**
         * Specifies filters to use for the request.
         * 
         * @param filters Map of filter key, valid values to create filters.
         * @return this
         */
        @SuppressWarnings("unchecked")
        public T filters(Map<String, String[]> filters) {
            this.filters = filters;
            return (T) this;
        }

        /**
         * Specifies filter strategies to use for the request.
         * 
         * @param filterStrategies Map of filter key, strategy for the filter. Note that filters default strategy if not specified is OR.
         * @return this
         */
        @SuppressWarnings("unchecked")
        public T filterStrategies(Map<String, FilterValuesStrategy> filterStrategies) {
            this.filterStrategies = filterStrategies;
            return (T) this;
        }

        protected String[] getTypes() {
            if (this.classes == null) {
                return null;
            }
            List<String> types = new ArrayList<String>(this.classes.length);
            for (Class<?> clazz : classes) {
                if (clazz != null) {
                    types.add(MappingBuilder.indexTypeFromClass(clazz));
                }
            }
            if (types.isEmpty()) {
                return null;
            }
            return types.toArray(new String[types.size()]);
        }
    }

    /**
     * Helper to build count queries.
     * 
     * @author luc boutier
     */
    public class CountQueryHelperBuilder extends QueryHelperBuilder<CountQueryHelperBuilder> {
        private CountQueryHelperBuilder(String[] indices, String searchQuery) {
            super(indices, searchQuery);
        }

        private CountQueryHelperBuilder(String[] indices, String searchPrefix, String prefixField) {
            super(indices, searchPrefix, prefixField);
        }

        /**
         * Perform a count request.
         * 
         * @return The count response.
         */
        public CountResponse count() {
            CountRequestBuilder countRequestBuilder = esClient.getClient().prepareCount(this.indices);
            countRequestBuilder.setTypes();
            // Count query doesn't have filters, they must be managed as boolean query element.
            QueryBuilder countQueryBuilder = buildQueryFilters();
            countRequestBuilder.setQuery(countQueryBuilder);

            return countRequestBuilder.execute().actionGet();
        }

        private QueryBuilder buildQueryFilters() {
            if (filters == null) {
                return queryBuilder;
            }
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(queryBuilder);

            Map<String, List<QueryBuilder>> nestedQueryBuildersMap = new HashMap<String, List<QueryBuilder>>();

            for (Class<?> clazz : classes) {
                buildClassQueryFilters(nestedQueryBuildersMap, boolQueryBuilder, clazz);
            }

            for (Entry<String, List<QueryBuilder>> nestedQuery : nestedQueryBuildersMap.entrySet()) {
                BoolQueryBuilder nestedBoolQueryBuilder = new BoolQueryBuilder();

                for (QueryBuilder filterQueryBuilder : nestedQuery.getValue()) {
                    nestedBoolQueryBuilder.must(filterQueryBuilder);
                }

                boolQueryBuilder.must(QueryBuilders.nestedQuery(nestedQuery.getKey(), nestedBoolQueryBuilder));
            }

            return boolQueryBuilder;
        }

        private void buildClassQueryFilters(Map<String, List<QueryBuilder>> nestedQueryBuildersMap, BoolQueryBuilder boolQueryBuilder, Class<?> clazz) {
            if (clazz == null) {
                return;
            }

            List<IFilterBuilderHelper> filterBuilderHelpers = mappingBuilder.getFilters(clazz.getName());
            if (filterBuilderHelpers == null) {
                return;
            }

            for (IFilterBuilderHelper filterBuilderHelper : filterBuilderHelpers) {
                String esFieldName = filterBuilderHelper.getEsFieldName();
                if (filters.containsKey(esFieldName)) {
                    if (filterBuilderHelper.isNested()) {
                        List<QueryBuilder> nestedQueryBuilders = nestedQueryBuildersMap.get(filterBuilderHelper.getNestedPath());
                        nestedQueryBuilders.add(filterBuilderHelper.buildQuery(esFieldName, filters.get(esFieldName)));
                    } else {
                        boolQueryBuilder.must(filterBuilderHelper.buildQuery(esFieldName, filters.get(esFieldName)));
                    }
                }
            }
        }
    }

    /**
     * Helper to build search queries.
     * 
     * @author luc boutier
     */
    public class SearchQueryHelperBuilder extends QueryHelperBuilder<SearchQueryHelperBuilder> {
        private String fetchContext;
        private boolean facets = false;
        private String functionScore;

        private SearchQueryHelperBuilder(String[] indices, String searchQuery) {
            super(indices, searchQuery);
        }

        private SearchQueryHelperBuilder(String[] indices, String searchPrefix, String prefixField) {
            super(indices, searchPrefix, prefixField);
        }

        /**
         * Specifies the fetch context to use for the search (fetch context is not used for count).
         * 
         * @param fetchContext The fetch context to use for the query.
         * @return this
         */
        public SearchQueryHelperBuilder fetchContext(String fetchContext) {
            this.fetchContext = fetchContext;
            return this;
        }

        /**
         * Enable or disable facets computation for the search request.
         * 
         * @param fetchContext The fetch context to use for the query.
         * @return this
         */
        public SearchQueryHelperBuilder facets(boolean facets) {
            this.facets = facets;
            return this;
        }

        /**
         * Allows to define a script to perform a function score query.
         * 
         * @param functionScore The script for the function score.
         * @return this
         */
        public SearchQueryHelperBuilder functionScore(String functionScore) {
            this.functionScore = functionScore;
            return this;
        }

        /**
         * Execute a search query using the defined query.
         * 
         * @param from The start index of the search (for pagination).
         * @param size The maximum number of elements to return.
         */
        public SearchResponse search(int from, int size) {
            SearchRequestBuilder searchRequestBuilder = esClient.getClient().prepareSearch(this.indices);
            searchRequestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH).setSize(size).setFrom(from);
            if (functionScore == null) {
                searchRequestBuilder.setQuery(queryBuilder);
            } else {
                searchRequestBuilder.setQuery(QueryBuilders.functionScoreQuery(queryBuilder, ScoreFunctionBuilders.scriptFunction(functionScore)));
            }
            searchRequestBuilder.setTypes(getTypes());
            if (classes != null && classes.length > 0) {
                addFetchContext(searchRequestBuilder);
                for (Class<?> clazz : classes) {
                    addFilters(searchRequestBuilder, clazz);
                }
            }

            if (prefixField == null) {
                searchRequestBuilder.addSort(SortBuilders.scoreSort());
            } else {
                searchRequestBuilder.addSort(SortBuilders.fieldSort(prefixField));
            }
            return searchRequestBuilder.execute().actionGet();
        }

        private void addFetchContext(SearchRequestBuilder searchRequestBuilder) {
            if (fetchContext == null) {
                return;
            }

            List<String> includes = new ArrayList<String>();
            List<String> excludes = new ArrayList<String>();

            for (Class<?> clazz : classes) {
                if (clazz != null) {
                    // get the fetch context for the given type and apply it to the search
                    SourceFetchContext sourceFetchContext = mappingBuilder.getFetchSource(clazz.getName(), fetchContext);
                    if (sourceFetchContext != null) {
                        includes.addAll(sourceFetchContext.getIncludes());
                        excludes.addAll(sourceFetchContext.getExcludes());
                    } else {
                        LOGGER.warn("Unable to find fetch context <" + fetchContext + "> for class <" + clazz.getName() + ">. It will be ignored.");
                    }
                }
            }

            String[] inc = includes.isEmpty() ? null : includes.toArray(new String[includes.size()]);
            String[] exc = excludes.isEmpty() ? null : excludes.toArray(new String[excludes.size()]);
            searchRequestBuilder.setFetchSource(inc, exc);
        }

        private void addFilters(SearchRequestBuilder searchRequestBuilder, Class<?> clazz) {
            if (clazz == null) {
                return;
            }
            final List<FilterBuilder> esFilters = buildFilters(clazz.getName());
            FilterBuilder filter = null;
            if (esFilters.size() > 0) {
                filter = getAndFitler(esFilters);
                searchRequestBuilder.setPostFilter(filter);
            }
            if (facets) {
                if (filters == null) {
                    addFacets(new HashMap<String, String[]>(), clazz.getName(), searchRequestBuilder, filter);
                } else {
                    addFacets(filters, clazz.getName(), searchRequestBuilder, filter);
                }
            }
        }

        private void addFacets(Map<String, String[]> filters, String className, SearchRequestBuilder searchRequestBuilder, FilterBuilder filter) {
            final List<FacetBuilder> facets = buildFacets(className, filters.keySet());
            for (final FacetBuilder facet : facets) {
                if (filter != null) {
                    facet.facetFilter(filter);
                }
                searchRequestBuilder.addFacet(facet);
            }
        }

        private List<FilterBuilder> buildFilters(String className) {
            List<FilterBuilder> filterBuilders = new ArrayList<FilterBuilder>();

            if (filters == null) {
                return filterBuilders;
            }

            Map<String, List<FilterBuilder>> nestedFilterBuilders = new HashMap<String, List<FilterBuilder>>();
            List<IFilterBuilderHelper> filterBuilderHelpers = mappingBuilder.getFilters(className);
            if (filterBuilderHelpers == null) {
                return filterBuilders;
            }

            for (IFilterBuilderHelper filterBuilderHelper : filterBuilderHelpers) {
                String esFieldName = filterBuilderHelper.getEsFieldName();
                if (filters.containsKey(esFieldName)) {
                    if (filterBuilderHelper.isNested()) {
                        List<FilterBuilder> nestedFilters = nestedFilterBuilders.get(filterBuilderHelper.getNestedPath());
                        if (nestedFilters == null) {
                            nestedFilters = new ArrayList<FilterBuilder>(3);
                            nestedFilterBuilders.put(filterBuilderHelper.getNestedPath(), nestedFilters);
                        }
                        nestedFilters.addAll(buildFilters(filterBuilderHelper, esFieldName, filters.get(esFieldName), filterStrategies.get(esFieldName)));
                    } else {
                        filterBuilders.addAll(buildFilters(filterBuilderHelper, esFieldName, filters.get(esFieldName), filterStrategies.get(esFieldName)));
                    }
                }
            }

            for (Entry<String, List<FilterBuilder>> nestedFilters : nestedFilterBuilders.entrySet()) {
                filterBuilders.add(FilterBuilders.nestedFilter(nestedFilters.getKey(), getAndFitler(nestedFilters.getValue())));
            }

            return filterBuilders;
        }

        private List<FilterBuilder> buildFilters(IFilterBuilderHelper filterBuilderHelper, String esFieldName, String[] values, FilterValuesStrategy strategy) {
            if (strategy == null || FilterValuesStrategy.OR.equals(strategy)) {
                return Lists.newArrayList(filterBuilderHelper.buildFilter(esFieldName, values));
            }
            List<FilterBuilder> valuesFilters = Lists.newArrayList();
            for (String value : values) {
                valuesFilters.add(filterBuilderHelper.buildFilter(esFieldName, value));
            }
            return valuesFilters;
        }

        private FilterBuilder getAndFitler(List<FilterBuilder> filters) {
            if (filters.size() == 1) {
                return filters.get(0);
            }
            return FilterBuilders.andFilter(filters.toArray(new FilterBuilder[filters.size()]));
        }

        /**
         * Create a list of facets for the given type.
         * 
         * @param className The name of the class for which to create facets.
         * @param filters The set of facets to exclude from the facet creation.
         * @return a {@link List} of {@link AbstractFacetBuilder facet builders}.
         */
        private List<FacetBuilder> buildFacets(String className, Set<String> filters) {
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
}