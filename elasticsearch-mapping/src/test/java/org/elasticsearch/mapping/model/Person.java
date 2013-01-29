package org.elasticsearch.mapping.model;

import org.elasticsearch.annotation.ESObject;

/**
 * 
 * @author luc boutier
 */
@ESObject
public class Person {
	private String id;
	private String firstname;
	private String lastname;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
}