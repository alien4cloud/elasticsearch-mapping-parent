package org.elasticsearch.mapping;

import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;

/**
 * Build a term facet.
 * 
 * @author luc boutier
 */
public class TermsFacetBuilderHelper extends TermsFilterBuilderHelper implements IFacetBuilderHelper {
    private final int size;
    private final boolean allTerms;
    private final ComparatorType comparatorType;
    private final Object[] exclude;

    /**
     * Initialize from the configuration annotation.
     * 
     * @param nestedPath The path to the nested object if any.
     * @param esFieldName The name of the field on which to apply the filter.
     * @param termsFacet the configuration annotation.
     */
    public TermsFacetBuilderHelper(final String nestedPath, final String esFieldName, TermsFacet termsFacet) {
        super(nestedPath, esFieldName);
        this.size = termsFacet.size();
        this.allTerms = termsFacet.allTerms();
        this.comparatorType = termsFacet.comparatorType();
        this.exclude = termsFacet.exclude();
    }

    @Override
    public FacetBuilder buildFacet() {
        TermsFacetBuilder termsFacetBuilder = FacetBuilders.termsFacet(getEsFieldName()).field(getEsFieldName()).allTerms(allTerms).order(comparatorType)
                .size(size);
        if (exclude.length > 0) {
            termsFacetBuilder.exclude(exclude);
        }
        return termsFacetBuilder;
    }
}