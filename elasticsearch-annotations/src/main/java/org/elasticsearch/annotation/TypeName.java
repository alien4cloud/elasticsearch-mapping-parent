package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to override the type name of a class (default is the class name in lower case).
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface TypeName {
	/**
	 * The name of the object type in elastic search.
	 * 
	 * @return The name of the object type in elastic search.
	 */
	String typeName();
}