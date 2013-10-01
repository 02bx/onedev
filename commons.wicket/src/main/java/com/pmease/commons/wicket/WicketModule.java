package com.pmease.commons.wicket;

import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.WicketServlet;

import com.pmease.commons.editable.EditSupport;
import com.pmease.commons.loader.AbstractPluginModule;
import com.pmease.commons.wicket.editable.EditableResourceReference;

public class WicketModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();
		
		bind(WicketServlet.class).to(DefaultWicketServlet.class);
		bind(WicketFilter.class).to(DefaultWicketFilter.class);
		
		contributeFromPackage(EditSupport.class, EditableResourceReference.class);
	}

}
