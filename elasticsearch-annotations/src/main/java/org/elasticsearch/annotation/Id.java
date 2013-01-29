package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.elasticsearch.mapping.IndexType;
import org.elasticsearch.mapping.YesNo;

/**
 * Id field for elastic search.
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Id {
	IndexType index() default IndexType.no;

	YesNo store() default YesNo.no;
}