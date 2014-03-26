package org.elasticsearch.mapping;

import org.elasticsearch.annotation.query.RangeFilter;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;

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
     * @param esFieldName The name of the field on which to apply the filter.
     * @param rangeFilterAnnotation The annotation that contains the range definition.
     */
    public RangeFilterBuilderHelper(final String esFieldName, final RangeFilter rangeFilterAnnotation) {
        this(esFieldName, rangeFilterAnnotation.ranges());
    }

    /**
     * Initialize a {@link RangeFilterBuilderHelper} from the informations that contains it's definition.
     * 
     * @param esFieldName The name of the field on which to apply the filter.
     * @param ranges The range definition.
     */
    public RangeFilterBuilderHelper(final String esFieldName, final double[] ranges) {
        super(esFieldName);
        this.ranges = ranges;
        if (this.ranges.length < 2) {
            throw new IllegalArgumentException("Size of ranges must be at least 2.");
        }
        if (this.ranges.length % 2 != 0) {
            throw new IllegalArgumentException("Size of ranges must an even number.");
        }
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
}