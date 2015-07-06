package org.elasticsearch.annotation;

import org.elasticsearch.mapping.TermVector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow to define mapping for the _all field
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ESAll {
    /**
     * Set to yes to store actual field in the index, no to not store it. Defaults to no (note, the JSON document itself
     * is stored, and it can be retrieved from it).
     *
     * @return Yes or no (default is no).
     */
    boolean store() default false;

//    /**
//     * Possible values are no, yes, with_offsets, with_positions, with_positions_offsets.
//     *
//     * @return Defaults to no.
//     */
//    TermVector termVector() default TermVector.no;

    /**
     * The analyzer used to analyze the text contents when analyzed during indexing and when searching using a query string. Defaults to the globally configured
     * analyzer.
     *
     * @return
     */
    String analyser() default "";

//    /**
//     * The analyzer used to analyze the text contents when analyzed during indexing.
//     *
//     * @return
//     */
//    String indexAnalyzer() default "";
//
//    /**
//     * The analyzer used to analyze the field when part of a query string. Can be updated on an existing field.
//     *
//     * @return
//     */
//    String searchAnalyzer() default "";
}