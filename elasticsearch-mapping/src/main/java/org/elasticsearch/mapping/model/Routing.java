package org.elasticsearch.mapping.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * 
 * @author luc boutier
 */
@JsonAutoDetect
public class Routing {

	private Boolean required;
	private String path;

	public Routing() {}

	public Routing(final String path) {
		this.path = path;
	}

	public Routing(final String path, final Boolean required) {
		this.path = path;
		this.required = required;
	}

	public Boolean getRequired() {
		return this.required;
	}

	public void setRequired(final Boolean required) {
		this.required = required;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(final String path) {
		this.path = path;
	}
}