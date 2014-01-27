package org.elasticsearch.annotation.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Range facet allows to specify a set of ranges and get both the number of docs (count) that fall within each range,
 * and aggregated data either based on the field, or using another field.
 * </p>
 * <p>
 * This annotation is used to define the behavior of a default facet search.
 * </p>
 * 
 * @author luc boutier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface RangeFacet {
	/**
	 * <p>
	 * Defines the ranges for the range facet.
	 * </p>
	 * <p>
	 * Simplest definition contains 2 values (2 ranges, any value to 'to' and from 'from' to any value):<br>
	 * [to, from];
	 * </p>
	 * <p>
	 * You can configure more ranges: [to, from,to, from,to, from,to, from]
	 * </p>
	 * <p>
	 * The range facet always include the from parameter and exclude the to parameter for each range.
	 * </p>
	 * 
	 * @return
	 */
	double[] ranges();
}