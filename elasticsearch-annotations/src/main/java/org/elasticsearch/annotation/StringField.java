package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.elasticsearch.mapping.IndexType;
import org.elasticsearch.mapping.TermVector;
import org.elasticsearch.mapping.YesNo;

/**
 * The text based string type is the most basic type, and contains one or more characters. This annotation allows to
 * specify the mapping for this field.
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface StringField {
	/**
	 * Set to yes to store actual field in the index, no to not store it. Defaults to no (note, the JSON document itself
	 * is stored, and it can be retrieved from it).
	 * 
	 * @return Yes or no (default is no).
	 */
	YesNo store() default YesNo.no;

	/**
	 * Set to analyzed for the field to be indexed and searchable after being broken down into token using an analyzer.
	 * not_analyzed means that its still searchable, but does not go through any analysis process or broken down into
	 * tokens. no means that it won’t be searchable at all (as an individual field; it may still be included in _all).
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
	 * Boolean value if norms should be omitted or not. Defaults to false.
	 * 
	 * @return True to omit norms, false to not omit norms.
	 */
	boolean omitNorms() default false;

	/**
	 * Boolean value if term frequency and positions should be omitted. Defaults to false.
	 * 
	 * @return True to omit term frequency and positions, false to not omit.
	 */
	boolean omitTermFreqAndPositions() default false;

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
}
