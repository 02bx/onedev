package com.gitplex.server.web.component.menu;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.apache.wicket.markup.html.WebMarkupContainer;

public abstract class MenuItem implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Nullable
	public abstract String getIconClass();
	
	public abstract String getLabel();
	
	public abstract WebMarkupContainer newLink(String id);
	
}
