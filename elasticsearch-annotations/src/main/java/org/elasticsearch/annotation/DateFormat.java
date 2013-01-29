package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface DateFormat {
	/**
	 * The <a url="http://www.elasticsearch.org/guide/reference/mapping/date-format.html">date format</a>. Defaults to
	 * dateOptionalTime.
	 * 
	 * @return The date format if any, null will use elastic search default.
	 */
	String format();
}