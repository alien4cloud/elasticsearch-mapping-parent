package org.elasticsearch.mapping.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Allows to define a field to be a boost value field.
 * 
 * @author luc boutier
 */
@JsonAutoDetect
public class Boost {
	private String name;
	private float null_value = 1f;

	public Boost() {}

	public Boost(final String name) {
		this.name = name;
	}

	public Boost(final String name, final float null_value) {
		this.name = name;
		this.null_value = null_value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public float getNull_value() {
		return this.null_value;
	}

	public void setNull_value(final float null_value) {
		this.null_value = null_value;
	}
}