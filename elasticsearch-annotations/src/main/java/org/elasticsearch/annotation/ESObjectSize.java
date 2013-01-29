package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.elasticsearch.mapping.YesNo;

/**
 * Annotation to add to an object that is mapped to Elastic Search.
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ESObjectSize {
	YesNo store() default YesNo.no;
}