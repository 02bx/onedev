package com.gitplex.http;

import org.apache.shiro.web.filter.mgt.FilterChainManager;

import com.gitplex.commons.jetty.ServletConfigurator;
import com.gitplex.commons.loader.AbstractPluginModule;
import com.gitplex.commons.shiro.FilterChainConfigurator;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class HttpModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();
		
        contribute(ServletConfigurator.class, HttpServletConfigurator.class);

        contribute(FilterChainConfigurator.class, new FilterChainConfigurator() {

            @Override
            public void configure(FilterChainManager filterChainManager) {
                filterChainManager.createChain("/**/info/refs", "noSessionCreation, authcBasic");
                filterChainManager.createChain("/**/git-upload-pack", "noSessionCreation, authcBasic");
                filterChainManager.createChain("/**/git-receive-pack", "noSessionCreation, authcBasic");
            }
            
        });
	}

}
