package org.elasticsearch.mapping;

import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;

/**
 * Build a term facet.
 * 
 * @author luc boutier
 */
public class TermsFacetBuilderHelper implements IFacetBuilderHelper {
    private final String esFieldName;
    private final int size;
    private final boolean allTerms;
    private final ComparatorType comparatorType;
    private final Object[] exclude;

    /**
     * Initialize from the configuration annotation.
     * 
     * @param termsFacet the configuration annotation.
     */
    public TermsFacetBuilderHelper(final String esFieldName, TermsFacet termsFacet) {
        this.esFieldName = esFieldName;
        this.size = termsFacet.size();
        this.allTerms = termsFacet.allTerms();
        this.comparatorType = termsFacet.comparatorType();
        this.exclude = termsFacet.exclude();
    }

    @Override
    public String getEsFieldName() {
        return this.esFieldName;
    }

    @Override
    public FacetBuilder buildFacet() {
        TermsFacetBuilder termsFacetBuilder = FacetBuilders.termsFacet(esFieldName).field(esFieldName).allTerms(allTerms).order(comparatorType).size(size);
        if (exclude.length > 0) {
            termsFacetBuilder.exclude(exclude);
        }
        return termsFacetBuilder;
    }

    @Override
    public FilterBuilder buildAssociatedFilter(final String key, final String value) {
        return FilterBuilders.termFilter(key, value);
    }
}