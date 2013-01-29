package org.elasticsearch.mapping.model;

import org.elasticsearch.mapping.IndexType;
import org.elasticsearch.mapping.YesNo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * An id in elastic search.
 * 
 * @author luc boutier
 */
@JsonAutoDetect
public class Id {
	private String path;
	private IndexType index;
	private YesNo store;

	public Id() {}

	public Id(final String path) {
		this.path = path;
	}

	public Id(final String path, final IndexType index) {
		this.path = path;
		this.index = index;
	}

	public Id(final String path, final IndexType index, final YesNo store) {
		this.path = path;
		this.index = index;
		this.store = store;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public IndexType getIndex() {
		return this.index;
	}

	public void setIndex(final IndexType index) {
		this.index = index;
	}

	public YesNo getStore() {
		return this.store;
	}

	public void setStore(final YesNo store) {
		this.store = store;
	}
}