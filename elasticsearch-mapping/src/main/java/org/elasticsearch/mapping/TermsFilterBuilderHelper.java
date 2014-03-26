package org.elasticsearch.mapping;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

/**
 * Build a term facet.
 * 
 * @author luc boutier
 */
public class TermsFilterBuilderHelper extends AbstractFilterBuilderHelper implements IFilterBuilderHelper {

    /**
     * Initialize from the configuration annotation.
     * 
     * @param termsFacet the configuration annotation.
     */
    public TermsFilterBuilderHelper(final String esFieldName) {
        super(esFieldName);
    }

    @Override
    public FilterBuilder buildFilter(final String key, final String[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Filter values cannot be null or empty");
        }
        if (values.length == 1) {
            return FilterBuilders.termFilter(key, values[0]);
        } else {
            return FilterBuilders.inFilter(key, values);
        }
    }
}