package com.pmease.commons.web.component.tabbable;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

@SuppressWarnings("serial")
public class Tabbable extends Panel {
	
	public Tabbable(String id, List<Tab> tabs) {
		super(id);
		
		WebMarkupContainer container = new WebMarkupContainer("container");
		container.add(AttributeModifier.replace("class", getCssClasses()));
		add(container);
		
		container.add(new ListView<Tab>("tabs", tabs){

			@Override
			protected void populateItem(ListItem<Tab> item) {
				Tab tab = item.getModelObject();
				
				if (tab.isActive(item))
					item.add(AttributeModifier.append("class", "active"));

				tab.populate(item, "tab");
			}
			
		});
	}
	
	/**
	 * Get css classes applied to the outer &lt;ul&gt; element.
	 * 
	 * @return
	 * 			Css classes applied to the outer &lt;ul&gt; elemnet.
	 */
	protected String getCssClasses() {
		return "nav nav-tabs";
	}
	
}
