package org.elasticsearch.mapping;

/**
 * Defaults to true for analyzed fields, and to false for not_analyzed fields.
 * 
 * @author luc boutier
 */
public enum NormEnabled {
    TRUE, FALSE, DEFAULT;
}