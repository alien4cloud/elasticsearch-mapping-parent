package org.elasticsearch.annotation.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.elasticsearch.mapping.NoneEnumType;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;

/**
 * <p>
 * Allow to specify field facets that return the N most frequent terms.
 * </p>
 * <p>
 * This annotation is used to define the behavior of a default facet search.
 * </p>
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface TermsFacet {
    /**
     * The property sub-path if any.<br>
     * If {@link #keysEnum()} is provided, this will be used as property sub paths of the values of the map
     * that will be used as terms.
     * 
     * @return The property sub path if any.
     */
    String[] paths() default "";

    /**
     * If dealing with a map (that will be stored as is), specified an Enum of keys you want to be a facet<br>
     * This can be used in combination with {@link #paths()} if the values of the map are complex types
     * 
     * @return The property sub path if any.
     */
    Class<? extends Enum<?>> keysEnum() default NoneEnumType.class;

    /**
     * The number of terms to return
     * 
     * @return The number of terms to return (default is 10).
     */
    int size() default 10;

    /**
     * Allow to control the ordering of the terms facets, to be ordered by count, term, reverse_count or reverse_term.
     * The default is count.
     * 
     * @return The control on how the term facets are ordered.
     */
    ComparatorType comparatorType() default ComparatorType.COUNT;

    /**
     * Allow to get all the terms in the terms facet, ones that do not match a hit, will have a count of 0. Note, this
     * should not be used with fields that have many terms.
     * 
     * @return true or false, default is false.
     */
    boolean allTerms() default false;

    /**
     * Specify a set of terms that should be excluded from the terms facet request result.
     * 
     * @return Array of terms that should be excluded from the terms facet request result.
     */
    String[] exclude() default {};

}