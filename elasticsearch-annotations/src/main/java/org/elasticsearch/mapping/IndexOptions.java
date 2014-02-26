package org.elasticsearch.mapping;

/**
 * Allows to set the indexing options, possible values are docs (only doc numbers are indexed), freqs (doc numbers and term frequencies), and positions (doc
 * numbers, term frequencies and positions). Defaults to positions for analyzed fields, and to docs for not_analyzed fields. It is also possible to set it to
 * offsets (doc numbers, term frequencies, positions and offsets).
 * 
 * @author lucboutier
 */
public enum IndexOptions {
    docs, freqs, positions, DEFAULT;
}
