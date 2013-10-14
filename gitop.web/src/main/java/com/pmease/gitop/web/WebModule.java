package com.pmease.gitop.web;

import javax.inject.Singleton;

import com.pmease.commons.jersey.JerseyConfigurator;
import com.pmease.commons.jersey.JerseyEnvironment;
import com.pmease.commons.jetty.ServletConfigurator;
import com.pmease.commons.loader.AbstractPluginModule;
import com.pmease.commons.wicket.AbstractWicketConfig;
import com.pmease.gitop.core.validation.UserNameReservation;
import com.pmease.gitop.web.resource.TestResource;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class WebModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();
		
		// put your guice bindings here
		bind(AbstractWicketConfig.class).to(GitopWebApp.class);		
		bind(SitePaths.class).in(Singleton.class);
		
		contribute(ServletConfigurator.class, WebServletConfigurator.class);
		contribute(UserNameReservation.class, WebUserNameReservation.class);
		
		contribute(JerseyConfigurator.class, new JerseyConfigurator() {
			
			@Override
			public void configure(JerseyEnvironment environment) {
				environment.addComponentFromPackage(TestResource.class);
			}
			
		});
	}

}
