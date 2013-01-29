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
public @interface IndexName {
	/**
	 * The name of the field that will be stored in the index. Defaults to the property/field name.
	 * 
	 * @return Null for default, the name of the field to override.
	 */
	String indexName();
}
