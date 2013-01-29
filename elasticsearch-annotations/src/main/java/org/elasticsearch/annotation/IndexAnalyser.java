package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The text based string type is the most basic type, and contains one or more characters. This annotation allows to
 * specify the mapping for this field.
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface IndexAnalyser {
	/**
	 * The analyzer used to analyze the text contents when analyzed during indexing.
	 * 
	 * @return Name of the analyzer.
	 */
	String indexAnalyzer();
}