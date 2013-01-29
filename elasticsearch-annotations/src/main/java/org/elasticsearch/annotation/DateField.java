package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.elasticsearch.mapping.YesNo;

/**
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface DateField {
	/**
	 * Set to yes to store actual field in the index, no to not store it. Defaults to no (note, the JSON document itself
	 * is stored, and it can be retrieved from it).
	 * 
	 * @return Yes or no (default is no).
	 */
	YesNo store() default YesNo.no;

	/**
	 * Set to no if the value should not be indexed. In this case, store should be set to yes, since if it’s not indexed
	 * and not stored, there is nothing to do with it.
	 * 
	 * @return Yes or no (default is yes).
	 */
	YesNo index() default YesNo.yes;

	/**
	 * The precision step (number of terms generated for each number value). Defaults to 4.
	 * 
	 * @return The precision step.
	 */
	int precisionStep() default 4;

	/**
	 * The boost value. Defaults to 1.0.
	 * 
	 * @return The new boost value.
	 */
	float boost() default 1f;

	/**
	 * Should the field be included in the _all field (if enabled). Defaults to true or to the parent object type
	 * setting.
	 * 
	 * @return True if the field should be included in the global _all field (if enabled), false if not.
	 */
	boolean includeInAll() default true;

	/**
	 * Ignored a malformed number. Defaults to false.
	 * 
	 * @return Should elastic search ignore a malformed number.
	 */
	boolean ignoreMalformed() default false;

}