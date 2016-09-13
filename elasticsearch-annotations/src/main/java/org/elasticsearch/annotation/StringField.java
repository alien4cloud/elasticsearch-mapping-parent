package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.elasticsearch.mapping.IndexOptions;
import org.elasticsearch.mapping.IndexType;
import org.elasticsearch.mapping.NormEnabled;
import org.elasticsearch.mapping.NormLoading;
import org.elasticsearch.mapping.TermVector;

/**
 * The text based string type is the most basic type, and contains one or more characters. This annotation allows to
 * specify the mapping for this field.
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface StringField {
    String NULL_VALUE_NOT_SPECIFIED = "org.elasticsearch.annotation.StringField.NullValue.NotSpecified";

    /**
     * Set to yes to store actual field in the index, no to not store it. Defaults to no (note, the JSON document itself
     * is stored, and it can be retrieved from it).
     * 
     * @return Yes or no (default is no).
     */
    boolean store() default false;

    /**
     * Set to analyzed for the field to be indexed and searchable after being broken down into token using an analyzer.
     * not_analyzed means that its still searchable, but does not go through any analysis process or broken down into
     * tokens. no means that it won���t be searchable at all (as an individual field; it may still be included in _all).
     * 
     * @return Defaults to analyzed, any other value to change setting.
     */
    IndexType indexType() default IndexType.analyzed;

    /**
     * Possible values are no, yes, with_offsets, with_positions, with_positions_offsets.
     * 
     * @return Defaults to no.
     */
    TermVector termVector() default TermVector.no;

    /**
     * The boost value. Defaults to 1.0.
     * 
     * @return The new boost value.
     */
    float boost() default 1f;

    /**
     * When there is a (JSON) null value for the field, use the null_value as the field value. Defaults to not adding the field at all.
     * 
     * @return
     */
    String nullValue() default NULL_VALUE_NOT_SPECIFIED;

    /**
     * Boolean value if norms should be enabled or not. Defaults to true for analyzed fields, and to false for not_analyzed fields.
     * 
     * @return True to omit norms, false to not omit norms.
     */
    NormEnabled normsEnabled() default NormEnabled.DEFAULT;

    /**
     * Describes how norms should be loaded, possible values are eager and lazy (default). It is possible to change the default value to eager for all fields by
     * configuring the index setting index.norms.loading to eager.
     * 
     * @return
     */
    NormLoading normsLoading() default NormLoading.DEFAULT;

    /**
     * Allows to set the indexing options, possible values are docs (only doc numbers are indexed), freqs (doc numbers and term frequencies), and positions (doc
     * numbers, term frequencies and positions). Defaults to positions for analyzed fields, and to docs for not_analyzed fields. It is also possible to set it
     * to offsets (doc numbers, term frequencies, positions and offsets).
     * 
     * @return
     */
    IndexOptions indexOptions() default IndexOptions.DEFAULT;

    /**
     * The analyzer used to analyze the text contents when analyzed during indexing and when searching using a query string. Defaults to the globally configured
     * analyzer.
     * 
     * @return
     */
    String analyser() default "";

    /**
     * The analyzer used to analyze the text contents when analyzed during indexing.
     * 
     * @return
     */
    String indexAnalyzer() default "";

    /**
     * The analyzer used to analyze the field when part of a query string. Can be updated on an existing field.
     * 
     * @return
     */
    String searchAnalyzer() default "";

    /**
     * Should the field be included in the _all field (if enabled). Defaults to true or to the parent object type
     * setting.
     * 
     * @return True if the field should be included in the global _all field (if enabled), false if not.
     */
    boolean includeInAll() default true;

    /**
     * Set to a size where above the mentioned size the string will be ignored. Handly for generic not_analyzed fields
     * that should ignore long text.
     * 
     * @return The size above which to ignore the field.
     */
    int ignoreAbove() default -1;

    /**
     * Position increment gap between field instances with the same field name. Defaults to 0.
     * 
     * @return
     */
    int positionOffsetGap() default 0;


}