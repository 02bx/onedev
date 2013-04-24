package com.pmease.commons.product.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;

import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;
import com.pmease.commons.wicket.behavior.dropdown.DropdownPanel;
import com.pmease.commons.wicket.page.CommonPage;

@SuppressWarnings("serial")
public class HomePage extends CommonPage  {
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new Link<Void>("link") {

			@Override
			public void onClick() {
				setResponsePage(TestPage.class);
			}
			
		});
		
		DropdownPanel dropdown = new DropdownPanel("dropdown", false) {

			@Override
			protected Component newContent(String id) {
				return new Label(id, "Hello World");
			}
			
		};
		add(dropdown);
		add(new WebMarkupContainer("toggle").add(new DropdownBehavior(dropdown)));
		
		add(new BookmarkablePageLink<Void>("test", TestPage.class));
	}	
	
}