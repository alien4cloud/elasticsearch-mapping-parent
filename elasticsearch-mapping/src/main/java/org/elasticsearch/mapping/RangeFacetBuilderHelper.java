package org.elasticsearch.mapping;

import org.elasticsearch.annotation.query.RangeFacet;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.range.RangeFacetBuilder;

/**
 * Build a range facet.
 * 
 * @author luc boutier
 */
public class RangeFacetBuilderHelper implements IFacetBuilderHelper {
	private final String esFieldName;
	private final double[] ranges;

	/**
	 * Initialize a {@link RangeFacetBuilderHelper} from the annotation that contains it's definition.
	 * 
	 * @param rangeFacetAnnotation The annotation that contains the range definition.
	 */
	public RangeFacetBuilderHelper(final String esFieldName, final RangeFacet rangeFacetAnnotation) {
		this.esFieldName = esFieldName;
		this.ranges = rangeFacetAnnotation.ranges();
		if (this.ranges.length < 2) {
			throw new IllegalArgumentException("Size of ranges must be at least 2.");
		}
		if (this.ranges.length % 2 != 0) {
			throw new IllegalArgumentException("Size of ranges must an even number.");
		}
	}

	@Override
	public String getEsFieldName() {
		return this.esFieldName;
	}

	@Override
	public FacetBuilder buildFacet() {
		RangeFacetBuilder rangeFacetBuilder = new RangeFacetBuilder(esFieldName).field(esFieldName).addUnboundedFrom(
				this.ranges[0]);
		int i = 1;
		for (; i < this.ranges.length - 2; i += 2) {
			rangeFacetBuilder.addRange(this.ranges[i], this.ranges[i + 1]);
		}
		rangeFacetBuilder.addUnboundedTo(this.ranges[i]);
		return rangeFacetBuilder;
	}

	@Override
	public FilterBuilder buildAssociatedFilter(final String key, final String value) {
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
