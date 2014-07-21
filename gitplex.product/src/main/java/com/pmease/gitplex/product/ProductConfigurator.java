package com.pmease.gitplex.product;

import javax.inject.Inject;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;

import com.pmease.commons.jetty.ServerConfigurator;
import com.pmease.gitplex.core.setting.ServerConfig;
import com.pmease.gitplex.core.setting.SslConfig;

public class ProductConfigurator implements ServerConfigurator {

	private ServerConfig serverConfig;
	
	@Inject
	public ProductConfigurator(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void configure(Server server) {
		if (serverConfig.getHttpPort() != 0) {
			SocketConnector connector = new SocketConnector();
			connector.setPort(serverConfig.getHttpPort());
			server.addConnector(connector);
		}

		SslConfig sslConfig = serverConfig.getSslConfig();
		if (sslConfig != null) {
			SslSocketConnector sslConnector = new SslSocketConnector();
			sslConnector.setPort(sslConfig.getPort());
			
			sslConnector.setKeystore(sslConfig.getKeystorePath());
			sslConnector.setPassword(sslConfig.getKeystorePassword());
			sslConnector.setKeyPassword(sslConfig.getKeystoreKeyPassword());
			
			server.addConnector(sslConnector);
		}
	}

}
