package org.elasticsearch.annotation.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     * The property sub-path if any.
     * 
     * @return The property sub path if any.
     */
    String[] paths() default "";

    /**
     * Optional paths generator to override the paths property of the annotation for complex use-case or reflection etc.
     * 
     * @return the path generator.
     */
    Class<? extends IPathGenerator> pathGenerator() default IPathGenerator.DEFAULT.class;

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