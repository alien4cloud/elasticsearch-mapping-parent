package org.elasticsearch.mapping;

import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;

/**
 * Build a term facet.
 * 
 * @author luc boutier
 */
public class TermsFacetBuilderHelper extends TermsFilterBuilderHelper implements IFacetBuilderHelper {
    private final int size;
    private final boolean allTerms;
    private final Terms.Order comparatorType;
    private final String[] exclude;

    /**
     * Initialize from the configuration annotation.
     * 
     * @param isAnalyzed True if the filtered field is analyzed, false if not.
     * @param nestedPath The path to the nested object if any.
     * @param esFieldName The name of the field on which to apply the filter.
     * @param termsFacet the configuration annotation.
     */
    public TermsFacetBuilderHelper(final boolean isAnalyzed, final String nestedPath, final String esFieldName, TermsFacet termsFacet) {
        super(isAnalyzed, nestedPath, esFieldName);
        this.size = termsFacet.size();
        this.allTerms = termsFacet.allTerms();
        this.comparatorType = termsFacet.comparatorType().getOrder();
        this.exclude = termsFacet.exclude();
    }

    @Override
    public TermsBuilder buildFacet() {
        TermsBuilder termsFacetBuilder = AggregationBuilders.terms(getEsFieldName()).field(getEsFieldName()).size(size).order(comparatorType);
        if (exclude.length > 0) {
            termsFacetBuilder.exclude(exclude);
        }
        if (allTerms) {
            // This will return even if the facet has 0 count
            termsFacetBuilder.minDocCount(0L);
        }
        return termsFacetBuilder;
    }
}