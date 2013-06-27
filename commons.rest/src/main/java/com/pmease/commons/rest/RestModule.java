package com.pmease.commons.rest;

import com.pmease.commons.loader.AbstractPlugin;
import com.pmease.commons.loader.AbstractPluginModule;
import com.pmease.commons.util.EasyMap;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if
 * you've renamed this class.
 * 
 */
public class RestModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();

		install(new JerseyServletModule() {

			protected void configureServlets() {
				// Bind at least one resource here as otherwise Jersey will report error.
				bind(DummyResource.class);

				// Route all RESTful requests through GuiceContainer
				serve(getRestPath()).with(
						GuiceContainer.class, 
						EasyMap.of("com.sun.jersey.api.json.POJOMappingFeature", "true"));
			}
			
			protected String getRestPath() {
				return "/" + RestPlugin.REST_PATH + "/*";
			}
			
		});
	}

	@Override
	protected Class<? extends AbstractPlugin> getPluginClass() {
		return RestPlugin.class;
	}

}
