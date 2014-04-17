package org.elasticsearch.mapping;

import org.elasticsearch.annotation.query.RangeFilter;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;

/**
 * Build a range facet.
 * 
 * @author luc boutier
 */
public class RangeFilterBuilderHelper extends AbstractFilterBuilderHelper implements IFilterBuilderHelper {
    private final double[] ranges;

    /**
     * Initialize a {@link RangeFilterBuilderHelper} from the annotation that contains it's definition.
     * 
     * @param nestedPath The path to the nested object if any.
     * @param esFieldName The name of the field on which to apply the filter.
     * @param rangeFilterAnnotation The annotation that contains the range definition.
     */
    public RangeFilterBuilderHelper(final String nestedPath, final String esFieldName, final RangeFilter rangeFilterAnnotation) {
        this(nestedPath, esFieldName, rangeFilterAnnotation.ranges());
    }

    /**
     * Initialize a {@link RangeFilterBuilderHelper} from the informations that contains it's definition.
     * 
     * @param isNested If the field is part of a nested object.
     * @param esFieldName The name of the field on which to apply the filter.
     * @param ranges The range definition.
     */
    public RangeFilterBuilderHelper(final String nestedPath, final String esFieldName, final double[] ranges) {
        super(nestedPath, esFieldName);
        this.ranges = ranges;
        if (this.ranges.length < 2) {
            throw new IllegalArgumentException("Size of ranges must be at least 2.");
        }
        if (this.ranges.length % 2 != 0) {
            throw new IllegalArgumentException("Size of ranges must an even number.");
        }
    }

    @Override
    public String getEsFieldName() {
        return getFullPath();
    }

    @Override
    public FilterBuilder buildFilter(final String key, final String[] rangeValues) {
        if (rangeValues == null || rangeValues.length == 0) {
            throw new IllegalArgumentException("Filter values cannot be null or empty");
        }
        if (rangeValues.length == 1) {
            return buildSingleRangeFilter(key, rangeValues[0]);
        } else {
            FilterBuilder[] builders = new FilterBuilder[rangeValues.length];
            for (int i = 0; i < rangeValues.length; i++) {
                builders[i] = buildSingleRangeFilter(key, rangeValues[i]);
            }
            return FilterBuilders.orFilter(builders);
        }
    }

    private FilterBuilder buildSingleRangeFilter(String key, String value) {
        String[] values = value.split(" - ");
        if (value.length() == 0) {
            return null;
        }
        RangeFilterBuilder filterBuilder = FilterBuilders.rangeFilter(key);
        if (value.length() == 2) {
            filterBuilder.from(Double.valueOf(values[0]).doubleValue()).to(Double.valueOf(values[1]));
        } else {
            if (value.startsWith(values[0])) {
                filterBuilder.gte(Double.valueOf(values[0]).doubleValue());
            } else {
                filterBuilder.lt(Double.valueOf(values[0]).doubleValue());
            }
        }
        return filterBuilder;
    }

    @Override
    public QueryBuilder buildQuery(String key, String[] rangeValues) {
        if (rangeValues == null || rangeValues.length == 0) {
            throw new IllegalArgumentException("Filter values cannot be null or empty");
        }
        if (rangeValues.length == 1) {
            return buildSingleRangeQuery(key, rangeValues[0]);
        } else {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
            for (int i = 0; i < rangeValues.length; i++) {
                queryBuilder.should(buildSingleRangeQuery(key, rangeValues[i]));
            }
            queryBuilder.minimumNumberShouldMatch(1);
            return queryBuilder;
        }
    }

    private QueryBuilder buildSingleRangeQuery(String key, String value) {
        String[] values = value.split(" - ");
        if (value.length() == 0) {
            return null;
        }
        RangeQueryBuilder queryBuilder = QueryBuilders.rangeQuery(key);
        if (value.length() == 2) {
            queryBuilder.from(Double.valueOf(values[0]).doubleValue()).to(Double.valueOf(values[1]));
        } else {
            if (value.startsWith(values[0])) {
                queryBuilder.gte(Double.valueOf(values[0]).doubleValue());
            } else {
                queryBuilder.lt(Double.valueOf(values[0]).doubleValue());
            }
        }
        return queryBuilder;
    }
}