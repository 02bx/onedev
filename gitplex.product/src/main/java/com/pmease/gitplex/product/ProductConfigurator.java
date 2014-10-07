package com.pmease.gitplex.product;

import javax.inject.Inject;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;

import com.pmease.commons.jetty.ServerConfigurator;
import com.pmease.gitplex.core.setting.ServerConfig;
import com.pmease.gitplex.core.setting.SslConfig;

public class ProductConfigurator implements ServerConfigurator {

	private ServerConfig serverConfig;
	
	@Inject
	public ProductConfigurator(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}
	
	@Override
	public void configure(Server server) {
		if (serverConfig.getHttpPort() != 0) {
			ServerConnector connector = new ServerConnector(server);
			connector.addConnectionFactory(new HttpConnectionFactory());
			connector.setPort(serverConfig.getHttpPort());
			server.addConnector(connector);
		}

		SslConfig sslConfig = serverConfig.getSslConfig();
		if (sslConfig != null) {
			ServerConnector sslConnector = new ServerConnector(server);
			sslConnector.setPort(sslConfig.getPort());
			
			SslConnectionFactory sslConnectionFactory = new SslConnectionFactory();
			sslConnectionFactory.getSslContextFactory().setKeyStorePath(sslConfig.getKeystorePath());
			sslConnectionFactory.getSslContextFactory().setKeyStorePassword(sslConfig.getKeystorePassword());
			sslConnectionFactory.getSslContextFactory().setKeyManagerPassword(sslConfig.getKeystoreKeyPassword());
			
			server.addConnector(sslConnector);
		}
	}

}
