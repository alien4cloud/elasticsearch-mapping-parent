package org.elasticsearch.annotation.query;

import org.elasticsearch.search.aggregations.bucket.terms.Terms;

/**
 * This simulate old 1.7 API, it's created to simplify migration
 */
public enum ComparatorType {

    COUNT(Terms.Order.count(false)), REVERSE_COUNT(Terms.Order.count(true)), TERM(Terms.Order.term(false)), REVERSE_TERM(Terms.Order.term(true));

    private Terms.Order order;

    ComparatorType(Terms.Order order) {
        this.order = order;
    }

    public Terms.Order getOrder() {
        return order;
    }
}
