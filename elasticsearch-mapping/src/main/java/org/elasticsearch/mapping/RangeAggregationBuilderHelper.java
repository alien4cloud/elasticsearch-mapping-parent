package org.elasticsearch.mapping;

import java.util.Collections;
import java.util.List;

import org.elasticsearch.annotation.query.RangeFacet;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.RangeBuilder;

/**
 * Build a range facet aggregation.
 * 
 * @author luc boutier
 */
public class RangeAggregationBuilderHelper extends RangeFilterBuilderHelper implements IFacetBuilderHelper {
    private final double[] ranges;

    /**
     * Initialize a {@link RangeAggregationBuilderHelper} from the annotation that contains it's definition.
     * 
     * @param nestedPath The path to the nested object if any.
     * @param esFieldName The name of the field on which to apply the filter.
     * @param rangeFacetAnnotation The annotation that contains the range definition.
     */
    public RangeAggregationBuilderHelper(final String nestedPath, final String esFieldName, final RangeFacet rangeFacetAnnotation) {
        super(nestedPath, esFieldName, rangeFacetAnnotation.ranges());
        this.ranges = rangeFacetAnnotation.ranges();
    }

    @Override
    public List<AggregationBuilder> buildFacets() {
        RangeBuilder rangeFacetBuilder = AggregationBuilders.range(getEsFieldName()).field(getEsFieldName()).addUnboundedFrom(this.ranges[0]);
        int i = 1;
        for (; i < this.ranges.length - 2; i += 2) {
            rangeFacetBuilder.addRange(this.ranges[i], this.ranges[i + 1]);
        }
        rangeFacetBuilder.addUnboundedTo(this.ranges[i]);
        return Collections.singletonList(rangeFacetBuilder);
    }
}