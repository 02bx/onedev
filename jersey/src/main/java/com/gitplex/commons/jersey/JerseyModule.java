package com.gitplex.commons.jersey;

import javax.inject.Singleton;

import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.glassfish.jersey.server.ResourceConfig;

import com.gitplex.calla.loader.AbstractPluginModule;
import com.gitplex.commons.shiro.FilterChainConfigurator;
import com.gitplex.commons.jetty.ServletConfigurator;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class JerseyModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();
		
		bind(ResourceConfig.class).toProvider(ResourceConfigProvider.class).in(Singleton.class);
		
		contribute(ServletConfigurator.class, JerseyServletConfigurator.class);

		contribute(FilterChainConfigurator.class, new FilterChainConfigurator() {

			@Override
			public void configure(FilterChainManager filterChainManager) {
				filterChainManager.createChain("/rest/**", "noSessionCreation, authcBasic");
			}
			
		});
		
		contribute(JerseyConfigurator.class, new JerseyConfigurator() {
			
			@Override
			public void configure(ResourceConfig resourceConfig) {
				
			}
			
		});
		
	}

}
