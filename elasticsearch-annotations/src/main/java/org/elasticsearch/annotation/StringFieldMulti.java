package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allow to specifies multiple StringField on a unique field to map it on multiple es fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface StringFieldMulti {
    /** The main string field to manage. */
    StringField main();

    /** Name of the multi */
    String[] multiNames();

    /** The multiple fields to add to the mapping. */
    StringField[] multi();
}
