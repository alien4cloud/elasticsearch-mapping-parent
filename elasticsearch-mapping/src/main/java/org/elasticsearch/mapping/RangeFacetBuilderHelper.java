package org.elasticsearch.mapping;

import org.elasticsearch.annotation.query.RangeFacet;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.range.RangeFacetBuilder;

/**
 * Build a range facet.
 * 
 * @author luc boutier
 */
public class RangeFacetBuilderHelper extends RangeFilterBuilderHelper implements IFacetBuilderHelper {
    private final double[] ranges;

    /**
     * Initialize a {@link RangeFacetBuilderHelper} from the annotation that contains it's definition.
     * 
     * @param nestedPath The path to the nested object if any.
     * @param esFieldName The name of the field on which to apply the filter.
     * @param rangeFacetAnnotation The annotation that contains the range definition.
     */
    public RangeFacetBuilderHelper(final String nestedPath, final String esFieldName, final RangeFacet rangeFacetAnnotation) {
        super(nestedPath, esFieldName, rangeFacetAnnotation.ranges());
        this.ranges = rangeFacetAnnotation.ranges();
    }

    @Override
    public FacetBuilder buildFacet() {
        RangeFacetBuilder rangeFacetBuilder = new RangeFacetBuilder(getEsFieldName()).field(getEsFieldName()).addUnboundedFrom(this.ranges[0]);
        int i = 1;
        for (; i < this.ranges.length - 2; i += 2) {
            rangeFacetBuilder.addRange(this.ranges[i], this.ranges[i + 1]);
        }
        rangeFacetBuilder.addUnboundedTo(this.ranges[i]);
        return rangeFacetBuilder;
    }
}