package org.elasticsearch.mapping;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
//import org.elasticsearch.index.query.FilterBuilder;
//import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
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

    @Value("#{elasticsearchConfig['elasticSearch.prefix_max_expansions']}")
    public void setMaxExpansions(final int maxExpansions) {
        this.maxExpansions = maxExpansions;
    }

    /**
     * Create a {@link QueryBuilderHelper} to prepare a query on elastic search.
     * 
     * @return a {@link QueryBuilderHelper} instance.
     */
    public IQueryBuilderHelper buildQuery() {
        return new QueryBuilderHelper(mappingBuilder, esClient);
    }

    /**
     * Create a {@link QueryBuilderHelper} to prepare a match phrase prefix query based on the given search query on elastic search. If the search query is
     * empty
     * or null this falls back to a match all query.
     *
     * @param searchQuery The search query.
     * @return a {@link QueryBuilderHelper} instance.
     */
    public IQueryBuilderHelper buildQuery(String searchQuery) {
        return new QueryBuilderHelper(mappingBuilder, esClient, maxExpansions, searchQuery);
    }

    /**
     * Create a {@link QueryBuilderHelper} to prepare a prefix query based on the given search query on elastic search. If the search query is empty
     * or null this falls back to a match all query.
     *
     * @param prefixField The field to use for prefix.
     * @param searchQuery The search query to apply on the prefix.
     * @return a {@link QueryBuilderHelper} instance.
     */
    public IQueryBuilderHelper buildQuery(String prefixField, String searchQuery) {
        return new QueryBuilderHelper(mappingBuilder, esClient, prefixField, searchQuery);
    }

    public interface IQueryBuilderHelper<T extends IQueryBuilderHelper> {
        /**
         * Add classes to the query so we can use the filter annotations and facets (aggregation) annotations.
         *
         * @param classes The classes.
         * @return a Filterable query builder.
         */
        IFilterableQueryBuilderHelper types(Class<?>... classes);

        /**
         * Execute the given adapter to alter the query builder.
         *
         * @param queryBuilderAdapter the query builder consumer to alter the query.
         * @return current builder instance.
         */
        T alterQuery(QueryBuilderAdapter queryBuilderAdapter);

        /**
         * Set a script function to use for scoring
         *
         * @param functionScore The function to use for scoring.
         * @return current builder instance.
         */
        T scriptFunction(String functionScore);

        /**
         * Perform a count request on the given indices.
         *
         * @param indices the indices on which to perform count.
         * @param types The elastic search types on which to perform count.
         * @return The count response.
         */
        CountResponse count(String[] indices, String... types);

        /**
         * Return the current query builder.
         * 
         * @return The elasticsearch query builder.
         */
        QueryBuilder getQueryBuilder();
    }

    public interface IFilterableQueryBuilderHelper<T extends IFilterableQueryBuilderHelper> extends IQueryBuilderHelper<T> {

        /**
         * Set filters from user provided filters.
         *
         * @param customFilter user provided filters.
         * @return current instance.
         */
        T filters(QueryBuilder... customFilter);

        /**
         * Add filters to the current query.
         *
         * @param filters The filters to add the the query based on annotation defined filters (as a filtered query).
         * @param customFilters user provided filters to add (using and clause) to the annotation based filters.
         * @return current instance.
         */
        T filters(Map<String, String[]> filters, QueryBuilder... customFilters);

        /**
         * Add filters to the current query.
         *
         * @param filters The filters to add the the query based on annotation defined filters (as a filtered query).
         * @param filterStrategies The filter strategies to apply to filters.
         * @param customFilters user provided filters to add (using and clause) to the annotation based filters.
         * @return current instance.
         */
        T filters(Map<String, String[]> filters, Map<String, FilterValuesStrategy> filterStrategies, QueryBuilder... customFilters);

        /**
         * 
         * @param indices
         * @return
         */
        ISearchQueryBuilderHelper prepareSearch(String... indices);
    }

    public interface ISearchQueryBuilderHelper extends IFilterableQueryBuilderHelper<ISearchQueryBuilderHelper> {
        /**
         * Execute a search query using the defined query.
         *
         * @param from The start index of the search (for pagination).
         * @param size The maximum number of elements to return.
         */
        SearchResponse execute(int from, int size);

        /**
         * Get the underlying search request builder.
         *
         * @return The underlying search request builder.
         */
        SearchRequestBuilder getSearchRequestBuilder();

        /**
         * Set the aggregations for the given classes.
         */
        ISearchQueryBuilderHelper facets();

        /**
         * Set the aggregations for the given classes.
         */
        ISearchQueryBuilderHelper facets(List<IFacetBuilderHelper> facetBuilderHelpers);

        /**
         * Execute the given consumer to alter the search request builder.
         *
         * @param searchRequestBuilderConsumer the search request builder consumer to alter the search request.
         */
        ISearchQueryBuilderHelper alterSearchRequest(ISearchBuilderAdapter searchRequestBuilderConsumer);

        /**
         * Allows to define a sort field.
         *
         * @param fieldName Name of the field to sort.
         * @param desc Descending or Ascending
         * @return this
         */
        ISearchQueryBuilderHelper fieldSort(String fieldName, boolean desc);

        /**
         * Add a fetch context to the query.
         *
         * @param fetchContext The fetch context to add to the query.
         */
        ISearchQueryBuilderHelper fetchContext(String fetchContext);

        /**
         * Apply the fetch context to the given aggregation (BUT DOES NOT add it to the query).
         * 
         * @param fetchContext The fetch context to add to the aggregation.
         * @param topHitsBuilder The top hits aggregation builder on which to add fetch context include and excludes.
         * @return The search query builder helper with the top
         */
        ISearchQueryBuilderHelper fetchContext(String fetchContext, TopHitsBuilder topHitsBuilder);
    }

    public static class QueryBuilderHelper implements ISearchQueryBuilderHelper {
        protected final MappingBuilder mappingBuilder;
        protected final ElasticSearchClient esClient;
        protected QueryBuilder queryBuilder;
        protected String prefixField;
        protected Class<?>[] classes;
        protected Map<String, String[]> filters;
        protected SearchRequestBuilder searchRequestBuilder;
        private boolean fieldSort = false;

        private QueryBuilderHelper(MappingBuilder mappingBuilder, ElasticSearchClient esClient) {
            this.queryBuilder = QueryBuilders.matchAllQuery();
            this.mappingBuilder = mappingBuilder;
            this.esClient = esClient;
        }

        protected QueryBuilderHelper(MappingBuilder mappingBuilder, ElasticSearchClient esClient, int maxExpansions, String searchQuery) {
            this.queryBuilder = getOrMatchAll(searchQuery, () -> QueryBuilders.prefixQuery("_all", searchQuery));
            //this.queryBuilder = getOrMatchAll(searchQuery, () -> QueryBuilders.matchPhrasePrefixQuery("_all", searchQuery).maxExpansions(maxExpansions));
            this.mappingBuilder = mappingBuilder;
            this.esClient = esClient;
        }

        protected QueryBuilderHelper(MappingBuilder mappingBuilder, ElasticSearchClient esClient, String prefixField, String searchPrefix) {
            this.prefixField = prefixField;
            this.queryBuilder = getOrMatchAll(searchPrefix, () -> QueryBuilders.prefixQuery(prefixField, searchPrefix));
            this.mappingBuilder = mappingBuilder;
            this.esClient = esClient;
        }

        protected QueryBuilderHelper(QueryBuilderHelper from) {
            this.queryBuilder = from.queryBuilder;
            this.prefixField = from.prefixField;
            this.mappingBuilder = from.mappingBuilder;
            this.esClient = from.esClient;
        }

        private QueryBuilder getOrMatchAll(String search, Supplier<QueryBuilder> supplier) {
            if (search == null || search.trim().isEmpty()) {
                return QueryBuilders.matchAllQuery();
            }
            return supplier.get();
        }

        @Override
        public QueryBuilderHelper types(Class<?>... classes) {
            // you must set classes before you can set filters for them.
            this.classes = classes;
            wrapPrefixQueryIfNested();
            return this;
        }

        private void wrapPrefixQueryIfNested() {
            if (prefixField == null) {
                return;
            }

            List<IFilterBuilderHelper> filterBuilderHelpers = mappingBuilder.getFilters(classes[0].getName());
            for (IFilterBuilderHelper filterBuilderHelper : filterBuilderHelpers) {
                if (filterBuilderHelper.isNested() && prefixField.equals(filterBuilderHelper.getEsFieldName())) {
                    this.queryBuilder = QueryBuilders.nestedQuery(filterBuilderHelper.getNestedPath(), queryBuilder);
                    return;
                }
            }

            return;
        }

        @Override
        public QueryBuilderHelper alterQuery(QueryBuilderAdapter queryBuilderConsumer) {
            queryBuilder = queryBuilderConsumer.adapt(this.queryBuilder);
            if (searchRequestBuilder != null) {
                searchRequestBuilder.setQuery(queryBuilder);
            }
            return this;
        }

        @Override
        public QueryBuilderHelper scriptFunction(String functionScore) {
            if (functionScore != null) {
                this.alterQuery(query -> QueryBuilders.functionScoreQuery(query, ScoreFunctionBuilders.scriptFunction(functionScore)));
            }
            return this;
        }

        @Override
        public CountResponse count(String[] indices, String... types) {
            CountRequestBuilder countRequestBuilder = esClient.getClient().prepareCount(indices);
            if (types != null && types.length > 0) {
                countRequestBuilder.setTypes(types);
            }
            countRequestBuilder.setQuery(this.queryBuilder);
            return countRequestBuilder.execute().actionGet();
        }

        @Override
        public QueryBuilder getQueryBuilder() {
            return this.queryBuilder;
        }

        @Override
        public QueryBuilderHelper prepareSearch(String... indices) {
            this.searchRequestBuilder = esClient.getClient().prepareSearch();
            // default search type.
            this.searchRequestBuilder.setSearchType(SearchType.QUERY_THEN_FETCH);
            this.searchRequestBuilder.setQuery(queryBuilder);
            this.searchRequestBuilder.setIndices(indices);
            return this;
        }

        @Override
        public QueryBuilderHelper filters(QueryBuilder... customFilter) {
            this.queryBuilder = addFilters(queryBuilder, Lists.newArrayList(customFilter));
            return this;
        }

        @Override
        public QueryBuilderHelper filters(Map<String, String[]> filters, QueryBuilder... customFilters) {
            return this.filters(filters, null, customFilters);
        }

        @Override
        public QueryBuilderHelper filters(Map<String, String[]> filters, Map<String, FilterValuesStrategy> filterStrategies, QueryBuilder... customFilters) {
            this.filters = filters;
            if (classes != null && classes.length > 0) {
                QueryBuilder filteredQueryBuilder = addFilters(this.queryBuilder, classes[0], filters, filterStrategies, customFilters);
                if (filteredQueryBuilder != null) {
                    this.queryBuilder = filteredQueryBuilder;
                }
            }
            if (this.searchRequestBuilder != null) {
                this.searchRequestBuilder.setQuery(queryBuilder);
            }
            return this;
        }

        private QueryBuilder addFilters(QueryBuilder query, Class<?> clazz, Map<String, String[]> filters, Map<String, FilterValuesStrategy> filterStrategies,
                QueryBuilder... customFilters) {
            if (clazz == null) {
                return query;
            }
            final List<QueryBuilder> esFilters = buildFilters(clazz.getName(), filters, filterStrategies);
            if (customFilters != null) {
                for (QueryBuilder customFilter : customFilters) {
                    if (customFilter != null) {
                        esFilters.add(customFilter);
                    }
                }

            }
            return addFilters(query, esFilters);
        }

        private QueryBuilder addFilters(QueryBuilder query, final List<QueryBuilder> esFilters) {
            QueryBuilder filter = null;
            if (esFilters.size() > 0) {
/**********************
                filter = getAndFilter(esFilters);
                if (filter != null) {
                    query = QueryBuilders.filteredQuery(query, filter);
                }
***********************/
                BoolQueryBuilder result = QueryBuilders.boolQuery().must(query);
                for (QueryBuilder esFilter : esFilters) {
                   result = result.must(esFilter);
                }
                query = result;
            }
            return query;
        }

        private List<QueryBuilder> buildFilters(String className, Map<String, String[]> filters, Map<String, FilterValuesStrategy> filterStrategies) {
            List<QueryBuilder> filterBuilders = new ArrayList<QueryBuilder>();

            if (filters == null) {
                return filterBuilders;
            }

            if (filterStrategies == null) {
                filterStrategies = Maps.newHashMap();
            }

            Map<String, List<QueryBuilder>> nestedFilterBuilders = new HashMap<String, List<QueryBuilder>>();
            List<IFilterBuilderHelper> filterBuilderHelpers = mappingBuilder.getFilters(className);
            if (filterBuilderHelpers == null) {
                return filterBuilders;
            }

            for (IFilterBuilderHelper filterBuilderHelper : filterBuilderHelpers) {
                String esFieldName = filterBuilderHelper.getEsFieldName();
                if (filters.containsKey(esFieldName)) {
                    if (filterBuilderHelper.isNested()) {
                        List<QueryBuilder> nestedFilters = nestedFilterBuilders.get(filterBuilderHelper.getNestedPath());
                        if (nestedFilters == null) {
                            nestedFilters = new ArrayList<QueryBuilder>(3);
                            nestedFilterBuilders.put(filterBuilderHelper.getNestedPath(), nestedFilters);
                        }
                        nestedFilters.addAll(buildFilters(filterBuilderHelper, esFieldName, filters.get(esFieldName), filterStrategies.get(esFieldName)));
                    } else {
                        filterBuilders.addAll(buildFilters(filterBuilderHelper, esFieldName, filters.get(esFieldName), filterStrategies.get(esFieldName)));
                    }
                }
            }

            for (Entry<String, List<QueryBuilder>> nestedFilters : nestedFilterBuilders.entrySet()) {
                filterBuilders.add(QueryBuilders.nestedQuery(nestedFilters.getKey(), getAndFilter(nestedFilters.getValue())));
            }

            return filterBuilders;
        }

        private List<QueryBuilder> buildFilters(IFilterBuilderHelper filterBuilderHelper, String esFieldName, String[] values, FilterValuesStrategy strategy) {
            if (strategy == null || FilterValuesStrategy.OR.equals(strategy)) {
                return Lists.newArrayList(filterBuilderHelper.buildFilter(esFieldName, values));
            }
            List<QueryBuilder> valuesFilters = Lists.newArrayList();
            for (String value : values) {
                valuesFilters.add(filterBuilderHelper.buildFilter(esFieldName, value));
            }
            return valuesFilters;
        }

        private QueryBuilder getAndFilter(List<QueryBuilder> filters) {
            if (filters.size() == 1) {
                return filters.get(0);
            }
            return QueryBuilders.andQuery(filters.toArray(new QueryBuilder[filters.size()]));
        }

        @Override
        public SearchResponse execute(int from, int size) {
            searchRequestBuilder.setTypes(getTypes());
            if (prefixField == null) {
                if (!fieldSort) {
                    searchRequestBuilder.addSort(SortBuilders.scoreSort());
                }
            } else {
                searchRequestBuilder.addSort(SortBuilders.fieldSort(prefixField));
            }
            return searchRequestBuilder.setFrom(from).setSize(size).execute().actionGet();
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

        @Override
        public SearchRequestBuilder getSearchRequestBuilder() {
            return searchRequestBuilder;
        }

        @Override
        public QueryBuilderHelper alterSearchRequest(ISearchBuilderAdapter searchRequestBuilderConsumer) {
            searchRequestBuilderConsumer.adapt(this.searchRequestBuilder);
            return this;
        }

        @Override
        public QueryBuilderHelper fieldSort(String fieldName, boolean desc) {
            if (fieldName == null) {
                return this;
            }
            fieldSort = true;
            FieldSortBuilder sortBuilder = SortBuilders.fieldSort(fieldName);
            if (desc) {
                sortBuilder.order(SortOrder.DESC);
            } else {
                sortBuilder.order(SortOrder.ASC);
            }
            // TODO: change to use sortBuilder.unmappedType
            sortBuilder.ignoreUnmapped(true);
            searchRequestBuilder.addSort(sortBuilder);
            return this;
        }

        @Override
        public QueryBuilderHelper fetchContext(String fetchContext) {
            if (fetchContext == null) {
                return this;
            }

            String[][] incExc = includeExcludes(fetchContext);

            searchRequestBuilder.setFetchSource(incExc[0], incExc[1]);
            return this;
        }

        @Override
        public QueryBuilderHelper fetchContext(String fetchContext, TopHitsBuilder aggregation) {
            if (fetchContext == null) {
                return this;
            }

            String[][] incExc = includeExcludes(fetchContext);

            aggregation.setFetchSource(incExc[0], incExc[1]);
            return this;
        }

        private String[][] includeExcludes(String fetchContext) {
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
                        LOGGER.debug("Unable to find fetch context <" + fetchContext + "> for class <" + clazz.getName() + ">. It will be ignored.");
                    }
                }
            }

            String[] inc = includes.isEmpty() ? null : includes.toArray(new String[includes.size()]);
            String[] exc = excludes.isEmpty() ? null : excludes.toArray(new String[excludes.size()]);
            return new String[][] { inc, exc };
        }

        @Override
        public QueryBuilderHelper facets() {
            return facets(new ArrayList<>());
        }

        @Override
        public QueryBuilderHelper facets(List<IFacetBuilderHelper> facetBuilderHelpers) {
            Set<String> aggIds = Sets.newHashSet();
            for (Class<?> clazz : classes) {
                if (filters == null) {
                    addAggregations(new HashMap(), clazz.getName(), searchRequestBuilder, aggIds,facetBuilderHelpers);
                } else {
                    addAggregations(filters, clazz.getName(), searchRequestBuilder, aggIds,facetBuilderHelpers);
                }
            }
            return this;
        }

        private void addAggregations(Map<String, String[]> filters, String className, SearchRequestBuilder searchRequestBuilder, Set<String> aggIds,List<IFacetBuilderHelper> facetBuilderHelpers) {
            final List<AggregationBuilder> aggregations = buildAggregations(className, filters.keySet(),facetBuilderHelpers);
            for (AggregationBuilder aggregation : aggregations) {
                if (!aggIds.contains(aggregation.getName())) {
                    aggIds.add(aggregation.getName());
                    searchRequestBuilder.addAggregation(aggregation);
                }
            }
        }

        /**
         * Create a list of aggregations counts for the given type.
         *
         * @param className The name of the class for which to create facets.
         * @param filters The set of aggregations to exclude from the facet creation.
         * @return a {@link List} of {@link AggregationBuilder aggregation builders}.
         */
        private List<AggregationBuilder> buildAggregations(String className, Set<String> filters,List<IFacetBuilderHelper> externalHelpers) {
            final List<AggregationBuilder> aggregationBuilders = new ArrayList<AggregationBuilder>();

            List<IFacetBuilderHelper> facetBuilderHelpers = Lists.newArrayList();
            facetBuilderHelpers.addAll(externalHelpers);
            if (mappingBuilder.getFacets(className) != null) {
                facetBuilderHelpers.addAll(mappingBuilder.getFacets(className));
            }

            if (facetBuilderHelpers == null || facetBuilderHelpers.size() < 1) {
                return aggregationBuilders;
            }

            for (IFacetBuilderHelper facetBuilderHelper : facetBuilderHelpers) {
                if (filters == null || !filters.contains(facetBuilderHelper.getEsFieldName())) {
                    aggregationBuilders.addAll(facetBuilderHelper.buildFacets());
                }
            }

            return aggregationBuilders;
        }
    }
}
