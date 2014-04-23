package org.elasticsearch.mapping;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Build a term filter.
 * 
 * @author luc boutier
 */
public class TermsFilterBuilderHelper extends AbstractFilterBuilderHelper {
    private final boolean isAnalyzed;

    /**
     * Initialize the helper to build term filters.
     * 
     * @param isAnalyzed True if the filtered field is analyzed, false if not.
     * @param nestedPath The path to the nested object if any.
     * @param filterPath The path to the field to filter.
     */
    public TermsFilterBuilderHelper(final boolean isAnalyzed, final String nestedPath, final String filterPath) {
        super(nestedPath, filterPath);
        this.isAnalyzed = isAnalyzed;
    }

    @Override
    public FilterBuilder buildFilter(final String key, final String[] values) {
        preProcessValues(values);
        if (values.length == 1) {
            return FilterBuilders.termFilter(key, values[0]);
        }
        return FilterBuilders.inFilter(key, values);
    }

    @Override
    public QueryBuilder buildQuery(String key, String[] values) {
        preProcessValues(values);
        if (values.length == 1) {
            return QueryBuilders.termQuery(key, values[0]);
        }
        return QueryBuilders.inQuery(key, values);
    }

    private void preProcessValues(String[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Filter values cannot be null or empty");
        }
        if (isAnalyzed) {
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i] == null ? null : values[i].toLowerCase();
            }
        }
    }
}