package com.pmease.gitplex.web.assets;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

import de.agilecoders.wicket.core.util.Dependencies;

public class PageResourceReference extends JavaScriptResourceReference {
	private static final long serialVersionUID = 1L;

	public PageResourceReference() {
		super(PageResourceReference.class, "js/page.js");
	}

	@Override
	public Iterable<? extends HeaderItem> getDependencies() {
		return Dependencies.combine(
				super.getDependencies(),
				new PriorityHeaderItem(CssHeaderItem.forReference(AssetLocator.ICONS_CSS)),
				CssHeaderItem.forReference(AssetLocator.PAGE_CSS));
	}
	
	private static final PageResourceReference instance = new PageResourceReference();
	
	public static PageResourceReference get() {
		return instance;
	}
	
}
