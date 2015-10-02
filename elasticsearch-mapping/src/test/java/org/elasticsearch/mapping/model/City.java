package org.elasticsearch.mapping.model;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

/**
 * 
 * @author luc boutier
 */
@ESObject(ttl = true)
public class City {
	@Id
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}