package com.pmease.commons.wicket.assets.cookies;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class CookiesResourceReference extends JavaScriptResourceReference {

	private static final long serialVersionUID = 1L;

	public CookiesResourceReference() {
		super(CookiesResourceReference.class, "cookies.min.js");
	}

}
