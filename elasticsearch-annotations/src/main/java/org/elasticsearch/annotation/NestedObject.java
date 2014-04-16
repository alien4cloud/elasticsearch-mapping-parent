package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the complex type must be indexed as an nested type. See
 * http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-nested-type.html.
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface NestedObject {
    Class<?> nestedClass() default NestedObject.class;
}