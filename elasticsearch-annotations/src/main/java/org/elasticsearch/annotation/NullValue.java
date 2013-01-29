package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to override the index name for a property.
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface NullValue {
	/**
	 * When there is a (JSON) null value for the field, use the null_value as the field value. Defaults to not adding
	 * the field at all.
	 * 
	 * @return A default value as string for null field (for numeric or boolean fields this must be a numeric or boolean
	 *         value).
	 */
	String nullValue();
}
