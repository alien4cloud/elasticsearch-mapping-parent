package org.elasticsearch.mapping;

import java.util.List;

import org.elasticsearch.annotation.query.TermsFacet;
import com.google.common.collect.Lists;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.missing.MissingBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
//import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;

/**
 * Build a term facet.
 * 
 * @author luc boutier
 */
public class TermsAggregationBuilderHelper extends TermsFilterBuilderHelper implements IFacetBuilderHelper {
    private final int size;
    private final boolean allTerms;
    //private final ComparatorType comparatorType;
    private final String[] exclude;

    /**
     * Initialize from the configuration annotation.
     * 
     * @param isAnalyzed True if the filtered field is analyzed, false if not.
     * @param nestedPath The path to the nested object if any.
     * @param esFieldName The name of the field on which to apply the filter.
     * @param termsFacet the configuration annotation.
     */
    public TermsAggregationBuilderHelper(final boolean isAnalyzed, final String nestedPath, final String esFieldName, TermsFacet termsFacet) {
        super(isAnalyzed, nestedPath, esFieldName);
        this.size = termsFacet.size();
        this.allTerms = termsFacet.allTerms();
        //this.comparatorType = termsFacet.comparatorType();
        this.exclude = termsFacet.exclude();
    }

    @Override
    public List<AggregationBuilder> buildFacets() {
        TermsBuilder termsBuilder = AggregationBuilders.terms(getEsFieldName()).field(getEsFieldName()).size(size);
        MissingBuilder missingBuilder = AggregationBuilders.missing("missing_" + getEsFieldName()).field(getEsFieldName());
        // Elastic search has a bug with excludes so don't use it. https://github.com/elastic/elasticsearch/issues/18575
        // if (exclude != null) {
        // termsBuilder.exclude(exclude);
        // }
        return Lists.newArrayList(termsBuilder, missingBuilder);
    }
}