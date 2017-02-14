package com.gitplex.commons.wicket;

import org.apache.wicket.protocol.http.IWebApplicationFactory;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;

import com.gitplex.commons.wicket.websocket.WebSocketFilter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultWicketFilter extends WebSocketFilter {

	private final AbstractWicketConfig wicketConfig;
	
	@Inject
	public DefaultWicketFilter(AbstractWicketConfig wicketConfig, WebSocketPolicy webSocketPolicy) {
		super(webSocketPolicy);
		
		this.wicketConfig = wicketConfig;
		setFilterPath("");
	}
	
	@Override
	protected IWebApplicationFactory getApplicationFactory() {
		return new IWebApplicationFactory() {

			public WebApplication createApplication(WicketFilter filter) {
				return wicketConfig;
			}

			public void destroy(WicketFilter filter) {
				
			}
		};
	}

}
