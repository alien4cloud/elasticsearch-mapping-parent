package org.elasticsearch.annotation.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow to add the field to one or more search context.
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface FetchContext {
    /**
     * Defines the context for which the field should be added to the _source retrieved by elastic search.
     * 
     * @return an Array that contains all the search context in which the field shoul be added.
     */
    String[] contexts();

    /**
     * Flags that must match the contexts array and specifies for each context if the field should be included or excluded from the source.
     * 
     * @return and Array that containts true if the field should be included in the given context, or false if it should be excluded.
     */
    boolean[] include();
}