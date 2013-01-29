package org.elasticsearch.mapping.model;

import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * 
 * @author luc boutier
 */
@JsonAutoDetect
public class PropertyMapping {
	private String type;
	private IndexType index;
	private Float boost;
	private Boolean include_in_all;

	public PropertyMapping() {}

	public PropertyMapping(final String type) {
		this.type = type;
	}

	public PropertyMapping(final String type, final IndexType index) {
		this.type = type;
		this.index = index;
	}

	public PropertyMapping(final String type, final IndexType index, final Boolean include_in_all) {
		this.type = type;
		this.index = index;
		this.include_in_all = include_in_all;
	}

	public PropertyMapping(final String type, final IndexType index, final Float boost) {
		this.type = type;
		this.index = index;
		this.boost = boost;
	}

	public PropertyMapping(final String type, final IndexType index, final Float boost, final Boolean include_in_all) {
		this.type = type;
		this.index = index;
		this.boost = boost;
		this.include_in_all = include_in_all;
	}

	public String getType() {
		return this.type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public IndexType getIndex() {
		return this.index;
	}

	public void setIndex(final IndexType index) {
		this.index = index;
	}

	public Float getBoost() {
		return this.boost;
	}

	public void setBoost(final Float boost) {
		this.boost = boost;
	}

	public Boolean getInclude_in_all() {
		return this.include_in_all;
	}

	public void setInclude_in_all(final Boolean include_in_all) {
		this.include_in_all = include_in_all;
	}
}