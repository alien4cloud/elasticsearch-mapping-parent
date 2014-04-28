package org.elasticsearch.mapping;

/**
 * Defines the available strategies to be applied on filter creation when multiple values are defined.
 * 
 * @author luc boutier
 */
public enum FilterValuesStrategy {
    /** Default strategy, the field must contains all of the specified values. */
    AND,
    /** Default strategy, the field must contains at least one of the specified values. */
    OR
}