package org.elasticsearch.mapping;

/**
 * Exception throwed when the mapping is not correct.
 * 
 * @author luc boutier
 */
public class MappingException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public MappingException(String message) {
		super(message);
	}
}
