package com.pmease.commons.wicket.component.tabbable;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.google.common.base.Preconditions;

public class PageTab implements Tab {
	
	private static final long serialVersionUID = 1L;

	private final IModel<String> titleModel;
	
	private final Class<?>[] pageClasses;
	
	public PageTab(IModel<String> titleModel, Class<?>...pageClasses) {
		Preconditions.checkArgument(pageClasses.length > 0, "At least one page class has to be provided.");
		
		this.titleModel = titleModel;
		this.pageClasses = pageClasses;
	}
	
	protected final IModel<String> getTitleModel() {
		return titleModel;
	}
	
	protected final Class<?>[] getPageClasses() {
		return pageClasses;
	}
	
	/**
	 * Override this to provide your own logic of populating tab item (the &lt;li&gt; element).
	 * 
	 * @param item
	 * 			The item to populate.
	 * @param componentId
	 * 			Id of the component to add to the item. 
	 */
	@Override
	public void populate(ListItem<Tab> item, String componentId) {
		item.add(new PageTabComponent(componentId, this));
	}

	@Override
	public boolean isActive(ListItem<Tab> item) {
		for (Class<?> pageClass: pageClasses) {
			if (pageClass.isAssignableFrom(item.getPage().getClass())) 
				return true;
		}
		return false;
	}

}
