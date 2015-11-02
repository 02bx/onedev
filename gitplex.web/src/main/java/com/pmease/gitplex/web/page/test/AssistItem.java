package com.pmease.gitplex.web.page.test;

import java.io.Serializable;

import org.apache.wicket.Component;

public abstract class AssistItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String input;
	
	private final int cursor;
	
	public AssistItem(String input, int cursor) {
		this.input = input;
		this.cursor = cursor;
	}
	
	public String getInput() {
		return input;
	}

	public int getCursor() {
		return cursor;
	}

	protected abstract Component render(String id);
	
}
