package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ObjectField {

    /**
     * Override the class of annotated field, helpful when annotate a map but use another object class to perform mapping creation
     * 
     * @return class to override the class of annotated field
     */
    Class<?> objectClass() default ObjectField.class;

    /**
     * The enabled flag allows to disable parsing and indexing a named object completely. This is handy when a portion of the JSON document contains arbitrary
     * JSON which should not be indexed, nor added to the mapping
     * 
     * @return Yes or no (default is yes)
     */
    boolean enabled() default true;
}
