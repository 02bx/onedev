package com.pmease.gitop.core;

import java.util.EnumSet;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.pmease.commons.hibernate.HibernateFilter;
import com.pmease.commons.jetty.ServletConfigurator;
import com.pmease.gitop.core.hookcallback.PostReceiveServlet;
import com.pmease.gitop.core.hookcallback.PreReceiveServlet;

@Singleton
public class CoreServletConfigurator implements ServletConfigurator {

	private final HibernateFilter hibernateFilter;
	
	private final PreReceiveServlet preReceiveServlet;
	
	private final PostReceiveServlet postReceiveServlet;
	
	@Inject
	public CoreServletConfigurator(HibernateFilter hibernateFilter, 
	        PreReceiveServlet preReceiveServlet, PostReceiveServlet postReceiveServlet) {
		this.hibernateFilter = hibernateFilter;
		this.preReceiveServlet = preReceiveServlet;
		this.postReceiveServlet = postReceiveServlet;
	}
	
	@Override
	public void configure(ServletContextHandler context) {
		FilterHolder filterHolder = new FilterHolder(hibernateFilter);
		context.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));
		
		ServletHolder servletHolder = new ServletHolder(preReceiveServlet);
		context.addServlet(servletHolder, PreReceiveServlet.PATH + "/*");
        
		servletHolder = new ServletHolder(postReceiveServlet);
        context.addServlet(servletHolder, PostReceiveServlet.PATH + "/*");
	}

}
